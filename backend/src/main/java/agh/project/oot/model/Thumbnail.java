package agh.project.oot.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table("thumbnails")
@EqualsAndHashCode
public class Thumbnail {

    @Id
    private Long id;

    private byte[] data;

    private Long imageId;

    private String type;

    public Thumbnail() {}

    public Thumbnail(byte[] data) {
        this.data = data;
    }

    public Thumbnail(byte[] data, Long imageId) {
        this.data = data;
        this.imageId = imageId;
    }
    public static Thumbnail from(Image image) {
        return new Thumbnail(image.getData(), image.getId());
    }
}
