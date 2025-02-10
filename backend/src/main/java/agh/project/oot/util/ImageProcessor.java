package agh.project.oot.util;

import agh.project.oot.*;
import agh.project.oot.messages.GetThumbnailsMessage;
import agh.project.oot.model.*;
import agh.project.oot.service.ImageOrderService;
import agh.project.oot.service.ImageService;
import agh.project.oot.service.MessageSender;
import agh.project.oot.service.ThumbnailService;
import agh.project.oot.thumbnails.UnsupportedImageFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessor {
    private final ImageSink imageSink;
    private final ThumbnailService thumbnailService;
    private final ImageService imageService;
    private final ImageOrderService imageOrderService;
    private final SessionRepository sessionRepository;
    private final MessageSender messageSender;

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

    public Mono<Void> processAndNotifyMissingThumbnails() {
        log.info("Processing and notifying about missing thumbnails...");

        var processingAndNotifying = thumbnailService.generateMissingThumbnails()
                .flatMap(tuple -> processImage(tuple.getT2(), tuple.getT1()))
                .then();

        return imageOrderService.initializeImageOrder()
                .then(processingAndNotifying)
                .doOnSuccess(unused -> log.info("Finished notifying clients about all missing thumbnails."))
                .onErrorContinue((error, item) -> handleException(error));
    }

    public Mono<Void> processImage(Thumbnail thumbnail, Image image) {
        return getImageOrder(image)
                .switchIfEmpty(
                        imageOrderService.getNextImageOrder(image.getFolderId())
                                .flatMap(upgradedImageOrder -> imageService.updateImageOrder(image, upgradedImageOrder)
                                        .doOnError(e -> log.error("Error updating image order: {}", e.getMessage()))
                                        .thenReturn(upgradedImageOrder)
                                ))
                .flatMap(imageOrder ->
                        thumbnailService.updateThumbnailOrder(thumbnail, imageOrder)
                                .doOnError(e -> log.error("Error updating thumbnail order: {}", e.getMessage()))
                                .then(sendThumbnailForAll(thumbnail))
                )
                .then();
    }

    private Mono<Long> getImageOrder(Image image) {
        return Mono.justOrEmpty(image.getImageOrder());
    }

    public Mono<Void> sendThumbnailForAll(Thumbnail thumbnail) {
        var thumbnailType = thumbnail.getType();
        return Flux.fromIterable(sessionRepository.getSessions().values())
                .filter(sessionData -> sessionData.getThumbnailType() == thumbnailType)
                .flatMap(sessionData -> imageService.findFolderIdByImageId(thumbnail.getImageId())
                        .map(folderId -> folderId.equals(sessionData.getFolderId()))
                        .filter(isMatch -> isMatch)
                        .map(isMatch -> sessionData.getSession())
                )
                .flatMap(session ->
                       messageSender.sendMessage(session, new GetThumbnailsMessage(thumbnailType, Collections.singletonList(IconDto.from(thumbnail))))
                )
                .doOnError(error -> log.error("Failed to send thumbnail: {}", error.getMessage()))
                .then();
    }

    private void handleException(Throwable error) {
        log.error("Error processing image: {}", error.getMessage());
        sessionRepository.getSessions().values().forEach(session ->
               messageSender.sendErrorResponse(session.getSession(), error).subscribe()
        );
        if (error instanceof UnsupportedImageFormatException) {
            imageService.removeById(((UnsupportedImageFormatException) error).getId())
                    .doOnSuccess(success -> log.info("Unsupported type image removing completed successfully"))
                    .doOnError(err -> log.error("Unhandled error during image removing: {}", err.getMessage()))
                    .subscribe();
        }
    }

}
