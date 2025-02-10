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
    private ThumbnailType type;
    private Long thumbnailOrder;

    public Thumbnail() {}

    public Thumbnail(byte[] data, ThumbnailType type) {
        this.data = data;
        this.type = type;
    }

    public Thumbnail(byte[] data, Long imageId, ThumbnailType type) {
        this.data = data;
        this.imageId = imageId;
        this.type = type;
    }
}
