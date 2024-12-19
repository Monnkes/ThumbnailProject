package agh.project.oot;

public enum ResponseStatus {
    OK(200),
    BAD_REQUEST(400),
    UNSUPPORTED_MEDIA_TYPE(415);
    ResponseStatus(int status) {
    }
}
