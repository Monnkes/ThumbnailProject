package agh.project.oot.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ThumbnailDto extends IconDto {

    private Long id;

    private byte[] data;

    private Long imageId;

    public ThumbnailDto() {
    }

    public ThumbnailDto(Long id, byte[] data, Long imageId) {
        this.id = id;
        this.data = data;
        this.imageId = imageId;
    }

    public ThumbnailDto(byte[] data) {
        this.data = data;
    }
}
