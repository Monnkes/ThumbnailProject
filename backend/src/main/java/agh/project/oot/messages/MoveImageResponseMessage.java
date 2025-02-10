package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class MoveImageResponseMessage extends Message {
    private Long id;
    public MoveImageResponseMessage(Long id) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.MOVE_IMAGE_RESPONSE);
        this.id = id;
    }
}
