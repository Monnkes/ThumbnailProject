package agh.project.oot.thumbnails;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
@Slf4j
public class ThumbnailConverter {
    public Mono<Thumbnail> generateThumbnail(Image image, int width, int height, String type) {
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

    public Flux<Thumbnail> generateAllThumbnails(Image image) {
        return Flux.concat(
                generateThumbnail(image, 150, 150, "small"),
                generateThumbnail(image, 300, 300, "medium"),
                generateThumbnail(image, 600, 600, "big")
        );
    }
}
