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
import org.springframework.beans.factory.annotation.Value;

@Component
@Slf4j
public class ThumbnailConverter {

    @Value("${thumbnail.width}")
    private int thumbnailWidth;

    @Value("${thumbnail.height}")
    private int thumbnailHeight;

    @Value("${converter.mediumThumbnailScale}")
    private int mediumThumbnailScale;

    @Value("${converter.bigThumbnailScale}")
    private int bigThumbnailScale;

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
        }).onErrorResume(error -> Mono.error(new UnsupportedImageFormatException(error.getMessage(), image.getId())));
    }

    public Flux<Thumbnail> generateAllThumbnails(Image image) {
        return Flux.concat(
                generateThumbnail(image, thumbnailWidth, thumbnailHeight, SMALL),
                generateThumbnail(image, thumbnailWidth * mediumThumbnailScale, thumbnailHeight * mediumThumbnailScale, MEDIUM),
                generateThumbnail(image, thumbnailWidth * bigThumbnailScale, thumbnailHeight * bigThumbnailScale, BIG)
        );
    }
}
