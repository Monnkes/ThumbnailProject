package agh.project.oot.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table("thumbnails")
public class Thumbnail {

    @Id
    private Long id;

    private byte[] data;

    private Long imageId;

    public Thumbnail() {}

    public Thumbnail(byte[] data) {
        this.data = data;
    }

    public Thumbnail(byte[] data, Long id) {
        this.data = data;
        this.id = id;
    }
}
