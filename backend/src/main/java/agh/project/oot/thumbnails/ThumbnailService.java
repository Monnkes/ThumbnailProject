package agh.project.oot.thumbnails;

import agh.project.oot.database.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {
    private final ThumbnailConverter thumbnailConverter;
    private final ImageRepository imageRepository;
    private final ThumbnailRepository thumbnailRepository;

    public Mono<List<Long>> saveImages(List<byte[]> images, int width, int height) {
        Flux<byte[]> imageFlux = Flux.fromIterable(images);
        return thumbnailConverter.generateThumbnails(imageFlux, width, height)
                .zipWith(imageFlux)
                .flatMap(tuple -> {
                    byte[] thumbnailData = tuple.getT1();
                    byte[] originalImageData = tuple.getT2();

                    log.info("Processing next image...");

                    Image image = new Image();
                    image.setData(originalImageData);

                    return imageRepository.save(image)
                            .doOnSuccess(savedImage ->
                                    log.info("Image saved successfully with ID: {}", savedImage.getId()))
                            .flatMap(savedImage -> {
                                Thumbnail thumbnail = new Thumbnail();
                                thumbnail.setData(thumbnailData);
                                thumbnail.setImageId(savedImage.getId());

                                return thumbnailRepository.save(thumbnail)
                                        .doOnSuccess(savedThumbnail ->
                                                log.info("Thumbnail saved successfully with ID: {}", savedThumbnail.getId()))
                                        .map(Thumbnail::getId);
                            })
                            .doOnTerminate(() -> log.info("Image processing completed"))
                            .onErrorResume(e -> {
                                log.error("Error processing image: {}", e.getMessage());
                                return Mono.empty();
                            });
                })
                .collectList();
    }


    public Flux<Picture> getAllThumbnails() {
        return thumbnailRepository.findAll()
                .map(thumbnail -> new Picture(thumbnail, imageRepository))
                .switchIfEmpty(Mono.empty());
    }
}
