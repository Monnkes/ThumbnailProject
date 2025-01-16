package agh.project.oot.thumbnails;

import lombok.Getter;

@Getter
public class UnsupportedImageFormatException extends Throwable {
    private final Long id;
    public UnsupportedImageFormatException(String message, Long id) {
        super(message);
        this.id = id;
    }
}