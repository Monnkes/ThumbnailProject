package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class PlaceholderNumberMessage extends Message {
    private Long thumbnailsNumber;

    public PlaceholderNumberMessage(Long thumbnailsNumber) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.PLACEHOLDERS_NUMBER_RESPONSE);
        this.thumbnailsNumber = thumbnailsNumber;
    }
}
