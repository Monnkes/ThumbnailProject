package agh.project.oot.service;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.repository.ImageRepository;
import agh.project.oot.repository.ThumbnailRepository;
import agh.project.oot.thumbnails.ThumbnailConverter;
import agh.project.oot.thumbnails.UnsupportedImageFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final ThumbnailConverter thumbnailConverter;
    private final ImageRepository imageRepository;
    private final ThumbnailRepository thumbnailRepository;

    /**
     * Processes a list of images, generates thumbnails, and saves them to the database.
     *
     * @param images the list of images to process.
     * @return a Flux of saved thumbnails.
     */
    public Flux<Thumbnail> saveImagesAndSendThumbnails(List<Image> images) {
        return Flux.fromIterable(images)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::processSingleImage)
                .sequential();
    }

    /**
     * Handles the processing of a single image: generates a thumbnail and saves both the image and its thumbnail.
     *
     * @param image the image to process.
     * @return a Mono of the saved thumbnail.
     */
    private Mono<Thumbnail> processSingleImage(Image image) {
        return thumbnailConverter.generateThumbnail(image)
                .flatMap(thumbnail -> saveImageAndThumbnail(image, thumbnail))
                .onErrorResume(error -> {
                    log.error("Error processing image: {}", error.getMessage());
                    return Mono.error(error);
                });
    }

    /**
     * Saves an image and its corresponding thumbnail to the database.
     *
     * @param image the original image.
     * @param thumbnail the generated thumbnail data.
     * @return a Mono of the saved thumbnail.
     */
    private Mono<Thumbnail> saveImageAndThumbnail(Image image, Thumbnail thumbnail) {
        return imageRepository.save(image)
                .flatMap(savedImage -> {
                    thumbnail.setImageId(savedImage.getId());
                    return thumbnailRepository.save(thumbnail);
                })
                .doOnSuccess(savedThumbnail -> log.info("Thumbnail saved successfully with ID: {}", savedThumbnail.getId()))
                .doOnError(error -> log.error("Error saving thumbnail: {}", error.getMessage()));
    }

    /**
     * Retrieves all thumbnails from the database.
     *
     * @return a Flux of all thumbnails.
     */
    public Flux<Thumbnail> getAllThumbnails() {
        return thumbnailRepository.findAll()
                .publishOn(Schedulers.parallel());
    }

    /**
     * Retrieves an image associated with a given thumbnail ID.
     *
     * @param thumbnailId the ID of the thumbnail.
     * @return a Mono of the image associated with the thumbnail.
     */
    public Mono<Image> getImageByThumbnailId(Long thumbnailId) {
        return thumbnailRepository.findById(thumbnailId)
                .flatMap(thumbnail -> imageRepository.findById(thumbnail.getImageId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Image with ID " + thumbnail.getImageId() + " not found"))))
                .switchIfEmpty(Mono.error(new NoSuchElementException("Thumbnail with ID " + thumbnailId + " not found")));
    }
}
