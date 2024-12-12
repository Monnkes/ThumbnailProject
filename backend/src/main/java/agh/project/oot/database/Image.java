package agh.project.oot.database;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table("images")
public class Image {

    @Id
    private Long id;

    private byte[] data;

    public Image() {}
}
