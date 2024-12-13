package agh.project.oot.thumbnails;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
@Slf4j
public class ThumbnailConverter {
    private final int width;
    private final int height;

    public ThumbnailConverter(@Value("${thumbnail.width}") int width, @Value("${thumbnail.height}") int height) {
        this.width = width;
        this.height = height;
    }

    private Mono<Thumbnail> generateThumbnail(Image image) {
        return Mono.fromCallable(() -> {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(image.getData());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.of(inputStream)
                    .size(width, height)
                    .toOutputStream(outputStream);

            return new Thumbnail(outputStream.toByteArray());
        });
    }


    public Flux<Thumbnail> generateThumbnails(Flux<Image> imageFlux) {
        return imageFlux.flatMap(imageData ->
                generateThumbnail(imageData)
                        .doOnError(error -> {
                            log.error("Error processing image: {}", error.getMessage());
                        })
                        .onErrorResume(error -> Mono.empty())
        );
    }
}
