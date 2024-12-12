package agh.project.oot.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ImageDto extends IconDto {

    private Long id;

    private byte[] data;

    public ImageDto() {
    }

    public ImageDto(byte[] data) {
        this.data = data;
    }
}
