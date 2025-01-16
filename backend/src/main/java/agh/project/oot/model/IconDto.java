package agh.project.oot.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class IconDto {

    private Long id;

    private byte[] data;

    private Long iconOrder;

    public IconDto() {
    }

    public IconDto(Long id, byte[] data, Long iconOrder) {
        this.id = id;
        this.data = data;
        this.iconOrder = iconOrder;
    }

    public static IconDto from(Thumbnail thumbnail) {
        return new IconDto(thumbnail.getId(), thumbnail.getData(), thumbnail.getThumbnailOrder());
    }

    public static IconDto from(Image image) {
        return new IconDto(image.getId(), image.getData(), image.getImageOrder());
    }

    @Override
    public String toString() {
        return "Icon{" +
                "id=" + id +
                ", dataSize=" + data.length +
                '}';
    }
}
