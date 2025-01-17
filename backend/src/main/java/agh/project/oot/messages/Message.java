package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract sealed class Message permits GetImageMessage, GetThumbnailsMessage, InfoResponseMessage, PingMessage,
        PlaceholderNumberMessage, PongMessage, UploadImageMessage, UploadZipMessage {
    private ConnectionStatus connectionStatus;
    private ResponseStatus responseStatus;
    private MessageType messageType;

    public Message(ConnectionStatus connectionStatus, ResponseStatus responseStatus, MessageType messageType) {
        this.connectionStatus = connectionStatus;
        this.responseStatus = responseStatus;
        this.messageType = messageType;
    }
}
