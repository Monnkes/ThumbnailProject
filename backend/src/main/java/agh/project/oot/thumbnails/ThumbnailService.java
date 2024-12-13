package agh.project.oot.thumbnails;

import agh.project.oot.database.*;
import agh.project.oot.model.ImageDto;
import agh.project.oot.model.ThumbnailDto;
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

    public Flux<Thumbnail> saveImagesAndSendThumbnails(List<ImageDto> images) {
        Flux<ImageDto> imagesFlux = Flux.fromIterable(images);
        return thumbnailConverter.generateThumbnails(imagesFlux)
                .zipWith(imagesFlux)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(tuple -> {
                    ThumbnailDto thumbnailData = tuple.getT1();
                    ImageDto originalImageData = tuple.getT2();

                    Image image = new Image();
                    image.setData(originalImageData.getData());

                    return imageRepository.save(image)
                            .flatMap(savedImage -> {
                                Thumbnail thumbnail = new Thumbnail();
                                thumbnail.setData(thumbnailData.getData());
                                thumbnail.setImageId(savedImage.getId());

                                return thumbnailRepository.save(thumbnail)
                                        .map(savedThumbnail -> {
                                            log.info("Thumbnail saved successfully with ID: {}", savedThumbnail.getId());
                                            return savedThumbnail;
                                        });
                            })
                            .onErrorResume(e -> {
                                log.error("Error processing image: {}", e.getMessage());
                                return Mono.empty();
                            });
                })
                .sequential();
    }


    public Flux<Thumbnail> getAllThumbnails() {
        return thumbnailRepository.findAll()
                .parallel()
                .runOn(Schedulers.parallel())
                .sequential()
                .switchIfEmpty((Mono.error(new NoSuchElementException("Some Thumbnails were not found"))));
    }


    public Mono<Image> getImageByThumbnailId(Long thumbnailId) {
        return thumbnailRepository.findById(thumbnailId)
                .flatMap(thumbnail -> {
                    Long imageId = thumbnail.getImageId();
                    return imageRepository.findById(imageId)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Image with ID " + imageId + " not found")));
                })
                .switchIfEmpty(Mono.error(new NoSuchElementException("Thumbnail with ID " + thumbnailId + " not found")));
    }
}
