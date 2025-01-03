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

    public Mono<Image> saveImage(Image image) {
        return imageRepository.save(image)
                .doOnSuccess(savedImage -> log.info("Saved image with ID: {}", savedImage.getId()))
                .doOnError(error -> log.error("Error saving image: {}", error.getMessage()));
    }

    public Flux<Thumbnail> saveThumbnailsForImage(Image savedImage) {
        return thumbnailConverter.generateAllThumbnails(savedImage)
                .doOnNext(thumbnail -> thumbnail.setImageId(savedImage.getId()))
                .flatMap(thumbnailRepository::save)
                .filter(thumbnail -> "small".equalsIgnoreCase(thumbnail.getType()))
                .doOnNext(savedThumbnail -> log.info("Saved thumbnail with type: {} for image ID: {}", savedThumbnail.getType(), savedThumbnail.getImageId()))
                .doOnError(error -> log.error("Error saving thumbnail: {}", error.getMessage()));
    }

    public Flux<Thumbnail> saveImageAndThumbnails(Image image) {
        return saveImage(image)
                .flatMapMany(this::saveThumbnailsForImage);
    }

    /**
     * Retrieves all thumbnails from the database.
     *
     * @return a Flux of all thumbnails.
     */
    public Flux<Thumbnail> getAllThumbnails(String type) {
        return thumbnailRepository.findByType(type)
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
