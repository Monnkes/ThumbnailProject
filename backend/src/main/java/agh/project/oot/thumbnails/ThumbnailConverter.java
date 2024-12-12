package agh.project.oot.thumbnails;

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

    public ThumbnailConverter() {
    }

    private Mono<byte[]> generateThumbnail(byte[] imageData, int width, int height) {
        return Mono.fromCallable(() -> {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.of(inputStream)
                    .size(width, height)
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();
        });
    }


    public Flux<byte[]> generateThumbnails(Flux<byte[]> imageFlux, int width, int height) {
        return imageFlux.flatMap(imageData ->
                generateThumbnail(imageData, width, height)
                        .doOnError(error -> {
                            log.error("Error processing image: {}", error.getMessage());
                        })
                        .onErrorResume(error -> Mono.just(new byte[0]))
        );
    }
}
