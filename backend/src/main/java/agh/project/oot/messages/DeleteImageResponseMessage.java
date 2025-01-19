package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class DeleteImageResponseMessage extends Message {
    // TODO Consider using Long instance List
    private Long id;

    public DeleteImageResponseMessage(Long id) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.DELETE_IMAGE_RESPONSE);
        this.id = id;
    }
}
