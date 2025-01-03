package agh.project.oot.thumbnails;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static agh.project.oot.model.ThumbnailType.*;

@Component
@Slf4j
public class ThumbnailConverter {
    public Mono<Thumbnail> generateThumbnail(Image image, int width, int height, ThumbnailType type) {
        return Mono.fromCallable(() -> {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(image.getData());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.of(inputStream)
                    .size(width, height)
                    .toOutputStream(outputStream);

            Thumbnail thumbnail = new Thumbnail();
            thumbnail.setData(outputStream.toByteArray());
            thumbnail.setType(type);

            return thumbnail;
        }).onErrorResume(error -> Mono.error(new UnsupportedImageFormatException(error.getMessage())));
    }

    // TODO use sizes from properties
    public Flux<Thumbnail> generateAllThumbnails(Image image) {
        return Flux.concat(
                generateThumbnail(image, 150, 150, SMALL),
                generateThumbnail(image, 300, 300, MEDIUM),
                generateThumbnail(image, 600, 600, BIG)
        );
    }
}
