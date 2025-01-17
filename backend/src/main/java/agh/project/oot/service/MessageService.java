package agh.project.oot.service;

import agh.project.oot.*;
import agh.project.oot.messages.*;
import agh.project.oot.model.IconDto;
import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import agh.project.oot.thumbnails.UnsupportedImageFormatException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageService {
    private final ObjectMapper objectMapper;
    private final ImageSink imageSink;
    private final ThumbnailService thumbnailService;
    private final ImageService imageService;
    private final AtomicLong currentImageOrder = new AtomicLong(-1);
    private final SessionRepository sessionRepository;
    private final FolderService folderService;

    @Value("${controller.maxAttempts}")
    private int maxAttempts;

    @Value("${controller.minBackoff}")
    private int minBackoff;

    @Value("${controller.delay}")
    private int delay;

    @EventListener(ApplicationReadyEvent.class)
    public void listenForNewImages() {
        imageSink.getSink().asFlux()
                .flatMap(imageService::findById)
                .flatMap(image -> thumbnailService.saveThumbnailsForImage(image)
                        .flatMap(thumbnail -> processImage(thumbnail, image))
                )
                .publishOn(Schedulers.boundedElastic())
                .onErrorContinue((error, item) -> handleException(error))
                .subscribe(
                        success -> log.info("Image processing completed successfully"),
                        error -> log.error("Unhandled error during image processing: {}", error.getMessage())
                );
    }

    // TODO Please refactor it
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> processAndNotifyMissingThumbnails() {
        log.info("Processing and notifying about missing thumbnails...");

        return messageServiceSetup()
                .then(
                        thumbnailService.generateMissingThumbnails()
                                .flatMap(tuple -> processImage(tuple.getT2(), tuple.getT1()))
                                .then()
                )
                .doOnSuccess(unused -> log.info("Finished notifying clients about all missing thumbnails."))
                .onErrorContinue((error, item) -> handleException(error));
    }

    private Mono<Void> messageServiceSetup() {
        return imageService.getTopImageOrder()
                .doOnNext(currentImageOrder::set)
                .then();
    }


    private void handleException(Throwable error) {
        log.error("Error processing image: {}", error.getMessage());
        sessionRepository.getSessions().values().forEach(session ->
                sendErrorResponse(session.getSession(), error).subscribe()
        );
        if (error instanceof UnsupportedImageFormatException) {
            imageService.removeById(((UnsupportedImageFormatException) error).getId()).subscribe(
                    // TODO refactor it
                    success -> log.info("Image processing completed successfully"),
                    err -> log.error("Unhandled error during image processing: {}", error.getMessage())
            );
        }
    }

    private Mono<Void> processImage(Thumbnail thumbnail, Image image) {
        return getImageOrder(image)
                .switchIfEmpty(Mono.defer(() -> Mono.fromCallable(currentImageOrder::incrementAndGet)
                        .flatMap(upgradedImageOrder -> imageService.updateImageOrder(image, upgradedImageOrder)
                                .doOnError(e -> log.error("Error updating image order: {}", e.getMessage()))
                                .thenReturn(upgradedImageOrder)
                        )))
                .flatMap(imageOrder ->
                        thumbnailService.updateThumbnailOrder(thumbnail, imageOrder)
                                .doOnError(e -> log.error("Error updating thumbnail order: {}", e.getMessage()))
                                .then(sendGeneratedThumbnail(thumbnail))
                )
                .then();
    }

    public Mono<Long> getImageOrder(Image image) {
        return Mono.justOrEmpty(image.getImageOrder());
    }

    public Mono<Void> sendGeneratedThumbnail(Thumbnail thumbnail) {
        var thumbnailType = thumbnail.getType();
        return Flux.fromIterable(sessionRepository.getSessions().values())
                .filter(sessionData -> sessionData.getThumbnailType() == thumbnailType)
                .flatMap(sessionData -> imageService.getFolderIdByImageId(thumbnail.getImageId())
                        .map(folderId -> folderId.equals(sessionData.getFolderId()))
                        .filter(isMatch -> isMatch)
                        .map(isMatch -> sessionData.getSession())
                )
                .flatMap(session ->
                        sendMessage(session, new GetThumbnailsMessage(thumbnailType, Collections.singletonList(IconDto.from(thumbnail))))
                )
                .doOnError(error -> log.error("Failed to send thumbnail: {}", error.getMessage()))
                .then();
    }

    public Mono<Message> parseMessage(String payload) {
        return Mono.fromCallable(() -> {
                    Map<String, Object> messageMap = objectMapper.readValue(payload, new TypeReference<>() {
                    });
                    MessageType type = MessageType.valueOf((String) messageMap.get("type"));
                    return switch (type) {
                        case UPLOAD_IMAGES ->
                                new UploadImageMessage(objectMapper.convertValue(messageMap.get("imagesData"), new TypeReference<>() {
                                }));
                        case UPLOAD_ZIP ->
                                new UploadZipMessage(objectMapper.convertValue(messageMap.get("zipData"), new TypeReference<>() {
                                }));
                        case GET_THUMBNAILS ->
                                new GetThumbnailsMessage(ThumbnailType.valueOf((String) messageMap.get("thumbnailType")));
                        case GET_IMAGE ->
                                new GetImageMessage(objectMapper.convertValue(messageMap.get("ids"), new TypeReference<>() {
                                }));
                        case PONG -> new PingMessage();
                        case PLACEHOLDERS_NUMBER_RESPONSE, INFO_RESPONSE, PING ->
                                new InfoResponseMessage(ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    };
                })
                .onErrorResume(error -> {
                    log.error("Error parsing message", error);
                    return Mono.error(new IllegalArgumentException("Invalid message format", error));
                });
    }

    public Mono<Void> sendMessage(WebSocketSession session, Message message) {
        return Mono.defer(() -> {
                    if (!session.isOpen()) {
                        log.warn("Session is not open. Unable to send message. Session ID: {}", session.getId());
                        return Mono.empty();
                    }
                    try {
                        String jsonMessage = objectMapper.writeValueAsString(message);
                        session.sendMessage(new TextMessage(jsonMessage));

                        log.info("Message sent successfully. Session ID: {}", session.getId());
                        return Mono.empty();
                    } catch (IOException e) {
                        log.error("IOException while sending message. Session ID: {}, Error: {}", session.getId(), e.getMessage());
                        return Mono.error(e);
                    } catch (Exception e) {
                        log.error("Unexpected error while sending message. Session ID: {}, Error: {}", session.getId(), e.getMessage());
                        return Mono.error(e);
                    }
                })
                .doOnError(e -> log.error("Error occurred during message sending. Session ID: {}, Error: {}", session.getId(), e.getMessage()))
                .retryWhen(getRetrySpec())
                .then();
    }


    public Mono<Void> handleUploadImages(UploadImageMessage message) {
        Mono<Void> placeholdersMono = handleGeneratePlaceholders(message.getImagesData().size());

        Mono<Void> saveAndNotifyMono = Flux.fromIterable(message.getImagesData())
                .map(icon -> new Image(icon.getData()))
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(imageService::saveAndNotifyThumbnail)
                .sequential()
                .then();

        return placeholdersMono.then(saveAndNotifyMono);
    }

    public Mono<Void> handleUploadZip(UploadZipMessage message) {
        return Flux.create((FluxSink<Image> sink) -> {
                    try (InputStream is = new ByteArrayInputStream(message.getZipData());
                         ZipInputStream zis = new ZipInputStream(is)) {

                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (!entry.isDirectory()) {
                                byte[] fileBytes = zis.readAllBytes();
                                String fullPath = entry.getName(); // np. "folder1/folder2/file.jpg"

                                List<String> parts = List.of(fullPath.split("/"));
                                String fileName = parts.get(parts.size() - 1);
                                List<String> folderPath = parts.subList(0, parts.size() - 1);

                                AtomicReference<Long> parentId = new AtomicReference<>(0L);

                                for (String folderName : folderPath) {
                                    Long currentParentId = parentId.get();

                                    folderService.createFolderIfNotExists(folderName, currentParentId)
                                            .doOnNext(parentId::set)
                                            .block();
                                }

                                Long finalFolderId = parentId.get();

                                Image image = new Image(fileBytes);
                                image.setFolderId(finalFolderId);
                                sink.next(image);
                            }
                        }
                        sink.complete();
                    } catch (IOException e) {
                        sink.error(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(imageService::saveAndNotifyThumbnail)
                .sequential().then();
    }

    public Mono<Void> handleGeneratePlaceholders(Integer placeholdersNumber) {
        return Flux.fromIterable(sessionRepository.getSessions().values())
                .flatMap(sessionData -> sendMessage(sessionData.getSession(), new PlaceholderNumberMessage(placeholdersNumber)))
                .then();
    }

    public Mono<Void> handleGetAllThumbnails(WebSocketSession session, GetThumbnailsMessage message) {
        Long folderId = message.getFolderId();
        return imageService.getImagesByFolderId(folderId)
                .collectList()
                .flatMap(images -> {
                    if (images.isEmpty()) {
                        return Mono.empty();
                    }
                    List<Long> imageIds = images.stream()
                            .map(Image::getId)
                            .collect(Collectors.toList());

//                    return handleGeneratePlaceholders(imageIds.size())
//                            .then(
                                    return Flux.fromIterable(imageIds)
                                    .flatMap(imageId -> thumbnailService.findByImageIdAndType(imageId, message.getThumbnailType()))
                                    .collectList()
                                    .flatMap(thumbnails -> {
                                        try {
                                            message.setImagesData(thumbnails.stream()
                                                    .map(IconDto::from)
                                                    .collect(Collectors.toList()));
                                            return sendMessage(session, message);
                                        } catch (Exception e) {
                                            log.error("Error while sending thumbnail message", e);
                                            return Mono.empty();
                                        }
                                    })
                                    .doOnError(error -> log.error("Error getting thumbnails for folder {}", folderId, error))
                                    .onErrorResume(e -> {
                                        log.error("Fallback due to error", e);
                                        return Mono.error(e);
                                    });
                })
                .then();
    }

    public Mono<Void> handleGetImage(WebSocketSession session, GetImageMessage message) {
        Long imageId = message.getIds().getFirst();

        return thumbnailService.getImageByThumbnailId(imageId)
                .flatMap(imageData -> {
                    message.setImagesData(List.of(IconDto.from(imageData)));
                    return sendMessage(session, message);
                })
                .then();
    }

    public Mono<Void> sendPingWithDelay(WebSocketSession session, Message message) {
        return Mono.delay(Duration.ofSeconds(delay))
                .then(sendMessage(session, message));
    }

    public Mono<Void> sendBadRequest(WebSocketSession session, String errorMessage, ResponseStatus responseStatus) {

        return sendMessage(session, new InfoResponseMessage(responseStatus, errorMessage));
    }

    public Mono<Void> sendErrorResponse(WebSocketSession session, Throwable error) {
        if (error.getClass() == UnsupportedImageFormatException.class || error.getClass() == UnsupportedFormatException.class) {
            return sendBadRequest(session, "Internal error: " + error.getMessage(), ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return sendBadRequest(session, "Internal error: " + error.getMessage(), ResponseStatus.BAD_REQUEST);
    }

    public void afterConnectionEstablished(WebSocketSession session) {
        sendMessage(session,
                new InfoResponseMessage(ResponseStatus.OK, "Connection established"))
                .then(sendPingWithDelay(session, new PingMessage()))
                .subscribe(
                        success -> log.info("Initial connection setup completed"),
                        error -> log.error("Error during connection setup", error)
                );
    }

    private RetryBackoffSpec getRetrySpec() {
        return Retry.backoff(maxAttempts, Duration.ofSeconds(minBackoff))
                .doBeforeRetry(retrySignal -> log.info("Retrying send attempt #{}", retrySignal.totalRetries() + 1))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                        new RuntimeException("Retries exhausted", retrySignal.failure()));
    }
}
