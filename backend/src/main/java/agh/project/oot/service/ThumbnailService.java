package agh.project.oot.service;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import agh.project.oot.repository.ThumbnailRepository;
import agh.project.oot.thumbnails.ThumbnailConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final ThumbnailConverter thumbnailConverter;
    private final ImageService imageService;
    private final ThumbnailRepository thumbnailRepository;

    public Flux<Tuple2<Image, Thumbnail>> generateMissingThumbnails() {
        log.info("Generating missing thumbnails...");

        return imageService.findAllImages()
                .flatMap(image -> generateMissingThumbnailsForImage(image)
                        .map(thumbnail -> Tuples.of(image, thumbnail))
                );
    }

    // !TODO Transfer this logic to Database
    private Flux<Thumbnail> generateMissingThumbnailsForImage(Image image) {
        return Flux.fromStream(Stream.of(ThumbnailType.SMALL, ThumbnailType.MEDIUM, ThumbnailType.BIG))
                .flatMap(type -> thumbnailRepository.findByImageIdAndType(image.getId(), type)
                        .switchIfEmpty(thumbnailConverter.generateThumbnail(image, type.getWidth(), type.getHeight(), type)
                                .flatMap(thumbnail -> {
                                    thumbnail.setImageId(image.getId());
                                    return this.save(thumbnail);
                                })));
    }

    public Mono<Thumbnail> save(Thumbnail thumbnail) {
        return thumbnailRepository.save(thumbnail);
    }

    public Flux<Thumbnail> saveThumbnailsForImage(Image savedImage) {
        return thumbnailConverter.generateAllThumbnails(savedImage)
                .flatMapSequential(thumbnail -> {
                            thumbnail.setImageId(savedImage.getId());
                            return this.save(thumbnail);
                        }
                );
    }

    public Flux<Thumbnail> findAllThumbnailsByTypeAndFolder(ThumbnailType type, Pageable pageable, Long folderId) {
        int limit = pageable.getPageSize();
        int offset = (pageable.getPageNumber()-1) * pageable.getPageSize();
        return thumbnailRepository.findByTypeAndFolderPaginated(type, limit, offset, folderId.intValue())
                .publishOn(Schedulers.parallel());
    }

    public Flux<Thumbnail> findAllThumbnailsAfterDeleting(ThumbnailType type, Integer page, Integer pageSize, Long folderId) {
        int limit = page * pageSize;
        int offset = 0;
        return thumbnailRepository.findByTypeAndFolderPaginated(type, limit, offset, folderId.intValue())
                .publishOn(Schedulers.parallel());
    }

    public Flux<Thumbnail> findAllThumbnailsByImageId(Long imageId) {
        return thumbnailRepository.findByImageId(imageId)
                .publishOn(Schedulers.parallel());
    }

    public Mono<Thumbnail> findThumbnailByThumbnailId(Long thumbnailId) {
        return thumbnailRepository.findById(thumbnailId)
                .publishOn(Schedulers.parallel());
    }

    public Mono<Image> findImageByThumbnailId(Long thumbnailId) {
        return findThumbnailByThumbnailId(thumbnailId)
                .flatMap(thumbnail -> imageService.findById(thumbnail.getImageId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Image with ID " + thumbnail.getImageId() + " not found"))))
                .switchIfEmpty(Mono.error(new NoSuchElementException("Thumbnail with ID " + thumbnailId + " not found")));
    }

    public Flux<Thumbnail> removeAllThumbnailsByImageId(Long imageId) {
        return thumbnailRepository.removeAllByImageId(imageId)
                .publishOn(Schedulers.parallel());
    }

    public Mono<Boolean> updateThumbnailOrder(Thumbnail thumbnail, long thumbnailOrder) {
        thumbnail.setThumbnailOrder(thumbnailOrder);
        return thumbnailRepository.updateThumbnailOrder(thumbnail.getId(), thumbnailOrder)
                .map(rowsUpdated -> rowsUpdated > 0)
                .doOnError(error -> log.error("Error updating thumbnail order for thumbnailId: {}", thumbnail.getId(), error));
    }
}
