package agh.project.oot.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table("images")
@EqualsAndHashCode
public class Image{

    @Id
    private Long id;

    private byte[] data;

    @Column("image_order")
    private Long imageOrder;

    private Long folderId = 0L;

    public Image() {}

    public Image(byte[] data) {
        this.data = data;
    }

    public Image(byte[] data, Long id) {
        this.data = data;
        this.id = id;
    }

    public Image(byte[] data, Long id, Long folderId) {
        this.data = data;
        this.id = id;
        this.folderId = folderId;
    }
}
