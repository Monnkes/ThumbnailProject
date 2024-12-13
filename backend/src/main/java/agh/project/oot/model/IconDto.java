package agh.project.oot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IconDto {

    private Long id;

    private byte[] data;

    public IconDto() {
    }

    public IconDto(Long id, byte[] data) {
        this.id = id;
        this.data = data;
    }

    @Override
    public String toString() {
        return "Icon{" +
                "id=" + id +
                ", data size=" + data.length +
                '}';
    }
}
