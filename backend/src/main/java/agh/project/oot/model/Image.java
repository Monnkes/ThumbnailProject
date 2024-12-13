package agh.project.oot.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table("images")
public class Image{

    @Id
    private Long id;

    private byte[] data;

    public Image() {}

    public Image(byte[] data) {
        this.data = data;
    }

    public Image(byte[] data, Long id) {
        this.data = data;
        this.id = id;
    }
}
