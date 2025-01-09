package agh.project.oot.service;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import agh.project.oot.repository.ThumbnailRepository;
import agh.project.oot.thumbnails.ThumbnailConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final ThumbnailConverter thumbnailConverter;
    private final ImageService imageService;
    private final ThumbnailRepository thumbnailRepository;

    public Flux<Thumbnail> generateMissingThumbnails() {
        log.info("Generating missing thumbnails...");

        return imageService.findAllImages()
                .flatMap(this::generateMissingThumbnailsForImage);
    }

    private Flux<Thumbnail> generateMissingThumbnailsForImage(Image image) {
        return Flux.fromStream(Stream.of(ThumbnailType.SMALL, ThumbnailType.MEDIUM, ThumbnailType.BIG))
                .flatMap(type -> thumbnailRepository.findByImageIdAndType(image.getId(), type)
                        .switchIfEmpty(thumbnailConverter.generateThumbnail(image, type.getWidth(), type.getHeight(), type)
                                .flatMap(thumbnail -> {
                                    thumbnail.setImageId(image.getId());
                                    return this.save(thumbnail);
                                })));
    }


    public Flux<Thumbnail> saveThumbnailsForImage(Image savedImage) {
        return thumbnailConverter.generateAllThumbnails(savedImage)
                .flatMapSequential(thumbnail -> {
                            thumbnail.setImageId(savedImage.getId());
                            return this.save(thumbnail);
                        }
                );
    }

    /**
     * Retrieves all thumbnails from the database.
     *
     * @return a Flux of all thumbnails.
     */
    public Flux<Thumbnail> getAllThumbnailsByType(ThumbnailType type) {
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
                .flatMap(thumbnail -> imageService.findById(thumbnail.getImageId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Image with ID " + thumbnail.getImageId() + " not found"))))
                .switchIfEmpty(Mono.error(new NoSuchElementException("Thumbnail with ID " + thumbnailId + " not found")));
    }

    public Mono<Thumbnail> save(Thumbnail thumbnail) {
        return thumbnailRepository.save(thumbnail);
    }
}
