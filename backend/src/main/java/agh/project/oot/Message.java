package agh.project.oot;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class Message {
    private ConnectionStatus connectionStatus;
    private ResponseStatus responseStatus;
    private MessageType type;
    private List<byte[]> imagesData;
    private List<Integer> ids = null;
    private String details = null;

    public Message() {
    }

    public Message(List<byte[]> imagesData, MessageType type) {
        this(ConnectionStatus.CONNECTED, ResponseStatus.OK, imagesData, type);
    }

    public Message(ConnectionStatus connectionStatus, ResponseStatus responseStatus, List<byte[]> imagesData, MessageType type) {
        this(connectionStatus, responseStatus, imagesData, type, null);
    }

    public Message(ConnectionStatus connectionStatus, ResponseStatus responseStatus, List<byte[]> imagesData, MessageType type, String details) {
        this.responseStatus = responseStatus;
        this.connectionStatus = connectionStatus;
        this.type = type;
        this.imagesData = imagesData;
        this.details = details;
    }
}
