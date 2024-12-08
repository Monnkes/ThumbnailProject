package agh.project.oot.thumbnails;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class ThumbnailService {

    private Mono<byte[]> generateThumbnail(byte[] imageData, int width, int height) {
        return Mono.fromCallable(() -> {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(inputStream);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.of(image)
                    .size(width, height)
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();
        });
    }

    public Flux<byte[]> generateThumbnails(Flux<byte[]> imageFlux, int width, int height) {
        return imageFlux.flatMap(imageData ->
                generateThumbnail(imageData, width, height)
                        .onErrorResume(error -> {
                            System.err.println("Error processing image: " + error.getMessage());
                            return Mono.empty();
                        })
        );
    }

}
