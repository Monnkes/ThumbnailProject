package agh.project.oot.service;

import agh.project.oot.*;
import agh.project.oot.messages.*;
import agh.project.oot.messages.DeleteFolderMessage;
import agh.project.oot.messages.DeleteFolderResponseMessage;
import agh.project.oot.messages.DeleteImageMessage;
import agh.project.oot.messages.DeleteImageResponseMessage;
import agh.project.oot.messages.GetImageMessage;
import agh.project.oot.messages.GetNextPageMessage;
import agh.project.oot.messages.GetThumbnailsMessage;
import agh.project.oot.messages.MoveImageMessage;
import agh.project.oot.messages.MoveImageResponseMessage;
import agh.project.oot.messages.PingMessage;
import agh.project.oot.messages.UploadImageMessage;
import agh.project.oot.messages.UploadZipMessage;
import agh.project.oot.model.*;
import agh.project.oot.util.ImageProcessor;
import agh.project.oot.util.ZipResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageService {
    private final ThumbnailService thumbnailService;
    private final ImageService imageService;
    private final SessionRepository sessionRepository;
    private final FolderService folderService;
    private final ImageOrderService imageOrderService;
    private final ImageProcessor imageProcessor;
    private final MessageSender messageSender;
    private final ZipResolver zipResolver;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeImageProcessing() {
        imageProcessor.listenForNewImages();
    }

    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initializeNotifyMissingThumbnails() {
        return imageProcessor.processAndNotifyMissingThumbnails();
    }

    public Mono<Void> handleUploadImages(UploadImageMessage message) {
        var imagesData = message.getImagesData();

        Mono<Boolean> isLastPageMono = imageService.countImagesByFolderId(message.getFolderId())
                .map(count -> checkIfIsLastPage(message, (double) count));

        Mono<Void> placeholdersMono = isLastPageMono.flatMap(isLastPage -> isLastPage
                ? handleGeneratePlaceholdersAfterUpload((long) imagesData.size(), message.getPageable().getPageNumber())
                : Mono.empty()
        );

        Mono<Void> saveAndNotifyMono = Flux.fromIterable(imagesData)
                .map(icon -> new Image(icon.getData()))
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(imageService::saveAndNotifyThumbnail)
                .sequential()
                .then();

        return placeholdersMono.then(saveAndNotifyMono);
    }

    private boolean checkIfIsLastPage(UploadImageMessage message, double count) {
        int lastPage = (int) Math.ceil(count / message.getPageable().getPageSize());
        int page = message.getPageable().getPageNumber();
        return page == lastPage || lastPage == 0;
    }

    public Mono<Void> handleGetNextPage(WebSocketSession session, GetNextPageMessage message) {
        return imageService.countImagesByFolderId(message.getFolderId())
                .flatMap(count -> {
                    int pageSize = message.getPageable().getPageSize();
                    int totalPages = (int) Math.ceil((double) count / pageSize);
                    int pageNumber = Math.min(message.getPageable().getPageNumber() + 1, totalPages);

                    message.setPageable(PageRequest.of(pageNumber, pageSize));
                    sessionRepository.updateSessionPageNumber(session.getId(), pageNumber);

                    return messageSender.sendMessage(session, message);
                })
                .then();
    }

    public Mono<Void> handleUploadZip(UploadZipMessage message) {
        return Flux.using(
                        () -> new ZipInputStream(new ByteArrayInputStream(message.getZipData())),
                        zipResolver::processZipEntries,
                        this::closeQuietly
                )
                .subscribeOn(Schedulers.boundedElastic())
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(imageService::saveAndNotifyThumbnail)
                .sequential()
                .then(Mono.defer(() -> messageSender.sendFoldersForAll(message.getFolderId())));
    }

    private void closeQuietly(ZipInputStream zis) {
        try {
            zis.close();
        } catch (IOException error) {
            log.error("Error closing zip stream: {}", error.getMessage());
        }
    }

    public Mono<Void> handleGeneratePlaceholdersAfterUpload(Long placeholdersNumber, Integer pageNumber) {
        return Flux.fromIterable(sessionRepository.getSessions().values())
                .filter(sessionData -> sessionData.getPageNumber() != null && sessionData.getPageNumber().equals(pageNumber))
                .flatMap(sessionData -> messageSender.sendMessage(sessionData.getSession(), new PlaceholderNumberMessage(placeholdersNumber)))
                .then();
    }

    public Mono<Void> handleGeneratePlaceholdersForGettingThumbnails(Long placeholdersNumber, WebSocketSession session) {
        return Flux.fromIterable(sessionRepository.getSessions().values())
                .filter(sessionData -> sessionData.getSession().equals(session))
                .flatMap(sessionData -> messageSender.sendMessage(sessionData.getSession(), new PlaceholderNumberMessage(placeholdersNumber)))
                .then();
    }

    public Mono<Void> handleGetAllThumbnails(WebSocketSession session, GetThumbnailsMessage message) {
        Long folderId = message.getFolderId();

        Mono<Void> placeholdersGenerated = imageService.countImagesByFolderId(folderId)
                .flatMap(count -> {
                    int placeholdersCount = Math.min(
                            count.intValue() - message.getPageable().getPageSize() * (message.getPageable().getPageNumber() - 1),
                            message.getPageable().getPageSize()
                    );
                    placeholdersCount = Math.max(placeholdersCount, 0);
                    return handleGeneratePlaceholdersForGettingThumbnails((long) placeholdersCount, session);
                })
                .then();

        Mono<Void> foldersResponse = folderService.getSubfolders(folderId)
                .sort()
                .collectList()
                .flatMap(folders -> sendFolderResponseMessage(session, folders, folderId))
                .then();

        Mono<Void> thumbnailsGenerated = thumbnailService.findAllThumbnailsByTypeAndFolder(message.getThumbnailType(), message.getPageable(), folderId)
                .flatMap(thumbnail -> {
                    message.setImagesData(List.of(IconDto.from(thumbnail)));
                    return messageSender.sendMessage(session, message);
                })
                .doOnError(error -> log.error("Error getting thumbnails for folder {}", folderId, error))
                .onErrorResume(Mono::error)
                .then();

        return Mono.when(placeholdersGenerated, foldersResponse)
                .then(thumbnailsGenerated)
                .then(messageSender.sendMessage(session, new FetchingEndResponseMessage()));
    }

    private Mono<Void> sendFolderResponseMessage(WebSocketSession session, List<Folder> folders, Long folderId) {
        return folderService.getParentId(folderId)
                .flatMap(parentId -> messageSender.sendMessage(
                        session,
                        new FoldersResponseMessage(folders, folderId, parentId)));
    }

    public Mono<Void> handleGetImage(WebSocketSession session, GetImageMessage message) {
        Long imageId = message.getIds().getFirst();

        return thumbnailService.findImageByThumbnailId(imageId)
                .flatMap(imageData -> {
                    message.setImagesData(List.of(IconDto.from(imageData)));
                    return messageSender.sendMessage(session, message);
                })
                .then();
    }

    public Mono<Void> handleMoveImage(WebSocketSession session, MoveImageMessage message) {
        Long targetFolderId = message.getTargetFolderId();
        Long currentFolderId = message.getCurrentFolderId();

        return Flux.fromIterable(message.getImageIds())
                .flatMap(imageId -> thumbnailService.findImageByThumbnailId(imageId).flux()
                        .concatMap(image -> imageService.updateFolderId(image.getId(), targetFolderId)
                                .then(updateImageAndAllThumbnailsOrderId(image, targetFolderId))
                                .then(messageSender.sendMessage(session, new MoveImageResponseMessage(imageId)))
                        ))
                .then(imageOrderService.recountOrderManyImages(message.getImageIds(), currentFolderId))
                .onErrorResume(error -> {
                    log.error("Error moving image: {}", error.getMessage());
                    return messageSender.sendBadRequest(session, "Error moving image: " + error.getMessage(), ResponseStatus.BAD_REQUEST);
                });
    }

    private Mono<Void> updateImageAndAllThumbnailsOrderId(Image image, Long targetFolderId) {
        return imageOrderService.getNextImageOrder(targetFolderId)
                .flatMap(newOrder -> Mono.when(imageService.updateImageOrder(image, newOrder)
                        , imageOrderService.recountThumbnailOrder(image.getId(), newOrder))
                );
    }

    public Mono<Void> handleDeleteImage(DeleteImageMessage message) {
        return thumbnailService.findThumbnailByThumbnailId(message.getId())
                .map(Thumbnail::getImageId)
                .flatMap(imageId -> thumbnailService.removeAllThumbnailsByImageId(imageId)
                        .then(imageOrderService.recountImageOrder(imageId))
                        .then(removeImageAndSendDeleteMessageResponse(message.getId(), imageId))
                )
                .then(messageSender.sendDeleteResponseMessage(message.getPageSize()));
    }

    public Mono<Void> handleDeleteFolder(DeleteFolderMessage message) {
        Long folderId = message.getId();

        return folderService.getSubfolders(folderId)
                .flatMap(subfolder -> handleDeleteFolder(new DeleteFolderMessage(subfolder.getId(), message.getPageSize())))
                .then(imageService.findImagesByFolderIdOrderByImageOrder(folderId)
                        .flatMap(image -> deleteImageAndAllThumbnails(message, image))
                        .then(Mono.when(folderService.deleteFolderById(folderId),
                                messageSender.sendDeleteMessageResponse(new DeleteFolderResponseMessage(folderId)))
                        )
                )
                .then(messageSender.sendDeleteResponseMessage(message.getPageSize()));
    }

    private Mono<Void> deleteImageAndAllThumbnails(DeleteFolderMessage message, Image image) {
        return thumbnailService.removeAllThumbnailsByImageId(image.getId())
                .then(imageOrderService.recountImageOrder(image.getId()))
                .then(removeImageAndSendDeleteMessageResponse(message.getId(), image.getId()));
    }

    private Mono<Void> removeImageAndSendDeleteMessageResponse(Long responseId, Long imageId) {
        return Mono.when(
                imageService.removeById(imageId),
                messageSender.sendDeleteMessageResponse(new DeleteImageResponseMessage(responseId))
        );
    }

    public void afterConnectionEstablished(WebSocketSession session) {
        messageSender.sendMessage(session, new InfoResponseMessage(ResponseStatus.OK, "Connection established"))
                .then(messageSender.sendPingWithDelay(session, new PingMessage()))
                .doOnSuccess(success -> log.info("Initial connection setup completed"))
                .doOnError(error -> log.error("Error during connection setup", error))
                .subscribe();
    }
}