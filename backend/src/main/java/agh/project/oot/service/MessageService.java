package agh.project.oot.service;

import agh.project.oot.*;
import agh.project.oot.model.IconDto;
import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import agh.project.oot.thumbnails.UnsupportedImageFormatException;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static agh.project.oot.MessageType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageService {
    private final ObjectMapper objectMapper;
    private final ImageSink imageSink;
    private final ThumbnailService thumbnailService;
    private final ImageService imageService;
    private final AtomicLong currentImageOrder = new AtomicLong(-1);
    private final SessionRepository sessionManager;

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

        return thumbnailService.generateMissingThumbnails()
                .flatMap(tuple -> processImage(tuple.getT2(), tuple.getT1()))
                .then()
                .doOnSuccess(unused -> log.info("Finished notifying clients about all missing thumbnails."))
                .onErrorContinue((error, item) -> handleException(error));
    }

    private void handleException(Throwable error) {
        log.error("Error processing image: {}", error.getMessage());
        sessionManager.getSessions().values().forEach(session ->
                sendErrorResponse(session.getSession(), error).subscribe()
        );
        if (error instanceof UnsupportedImageFormatException) {
            imageService.removeById(((UnsupportedImageFormatException) error).getId()).subscribe(
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
        return Flux.fromIterable(sessionManager.getSessions().values())
                .filter(sessionData -> sessionData.getThumbnailType().equals(thumbnailType))
                .map(SessionData::getSession)
                .flatMap(session ->
                        sendMessage(session, new Message(Collections.singletonList(IconDto.from(thumbnail)),
                                GET_THUMBNAILS_RESPONSE, thumbnailType
                        ))
                )
                .doOnError(error -> log.error("Failed to send thumbnail: {}", error.getMessage()))
                .then();
    }

    public Mono<Message> parseMessage(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, Message.class))
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


    public Mono<Void> handleUploadImages(Message request) {
        Mono<Void> placeholdersMono = handleGeneratePlaceholders(request.getImagesData().size());

        Mono<Void> saveAndNotifyMono = Flux.fromIterable(request.getImagesData())
                .map(icon -> new Image(icon.getData()))
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(imageService::saveAndNotifyThumbnail)
                .sequential()
                .then();

        return Mono.when(placeholdersMono, saveAndNotifyMono);
    }

    public Mono<Void> handleGeneratePlaceholders(Integer placeholdersNumber) {
        return Flux.fromIterable(sessionManager.getSessions().values())
                .flatMap(sessionData -> sendMessage(sessionData.getSession(), new Message(INFO_RESPONSE, placeholdersNumber)))
                .then();
    }

    public Mono<Void> handleGetAllThumbnails(WebSocketSession session, ThumbnailType thumbnailType) {
        return thumbnailService.getAllThumbnailsByType(thumbnailType)
                .flatMap(thumbnail -> {
                    try {
                        return sendMessage(session, new Message(
                                Collections.singletonList(IconDto.from(thumbnail)), GET_THUMBNAILS_RESPONSE, thumbnailType
                        ));
                    } catch (Exception e) {
                        log.error("Error while sending thumbnail message", e);
                        return Mono.empty();
                    }
                })
                .doOnComplete(() -> log.info("All initial thumbnails sent successfully"))
                .doOnError(error -> log.error("Error getting thumbnails", error))
                .onErrorResume(e -> {
                    log.error("Fallback due to error", e);
                    return Mono.error(e);
                })
                .then();
    }

    public Mono<Void> handleGetImage(WebSocketSession session, Message request) {
        Long imageId = request.getIds().getFirst();

        return thumbnailService.getImageByThumbnailId(imageId)
                .flatMap(imageData -> sendMessage(session, new Message(
                        List.of(IconDto.from(imageData)), MessageType.GET_IMAGE_RESPONSE, null
                )))
                .then();
    }

    public Mono<Void> sendPingWithDelay(WebSocketSession session) {
        return Mono.delay(Duration.ofSeconds(delay))
                .then(sendMessage(session, new Message(null, MessageType.PING, null)));
    }

    public Mono<Void> sendBadRequest(WebSocketSession session, String errorMessage, ResponseStatus responseStatus) {
        return sendMessage(session, new Message(ConnectionStatus.CONNECTED, responseStatus, null, null,
                MessageType.INFO_RESPONSE, errorMessage, null));
    }

    public Mono<Void> sendErrorResponse(WebSocketSession session, Throwable error) {
        if (error.getClass() == UnsupportedImageFormatException.class || error.getClass() == UnsupportedFormatException.class) {
            return sendBadRequest(session, "Internal error: " + error.getMessage(), ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return sendBadRequest(session, "Internal error: " + error.getMessage(), ResponseStatus.BAD_REQUEST);
    }

    public void afterConnectionEstablished(WebSocketSession session) {
        sendMessage(session,
                new Message(ConnectionStatus.CONNECTED, ResponseStatus.OK, null, null, MessageType.INFO_RESPONSE,
                        "Connection established", null))
                .then(sendPingWithDelay(session))
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
