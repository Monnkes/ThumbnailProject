package agh.project.oot.model;

import lombok.Getter;

@Getter
public enum ThumbnailType {
    //TODO transfer it to application.properties
    SMALL(150, 150),
    MEDIUM(300, 300),
    BIG(600, 600);

    private final int width;
    private final int height;

    ThumbnailType(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
