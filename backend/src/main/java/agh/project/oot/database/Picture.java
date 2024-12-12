package agh.project.oot.database;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Setter
public class Picture {

    @Getter
    private final Thumbnail thumbnail;
    private Image image;

    private final ImageRepository imageRepository;

    public Picture(Thumbnail thumbnail, ImageRepository imageRepository) {
        this.thumbnail = thumbnail;
        this.imageRepository = imageRepository;
        this.image = null;
    }

    public Picture(Thumbnail thumbnail, Image image, ImageRepository imageRepository) {
        this.thumbnail = thumbnail;
        this.image = image;
        this.imageRepository = imageRepository;
    }

    public Mono<Image> getImage() {
        if (this.image == null) {
            this.image = imageRepository.findById(thumbnail.getImageId()).block();
        }
        return Mono.just(this.image);
    }

}
