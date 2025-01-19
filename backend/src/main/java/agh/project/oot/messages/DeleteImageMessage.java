package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class DeleteImageMessage extends Message {
    // TODO Consider using Long instance List
    private Long id;

    public DeleteImageMessage(Long id) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.DELETE_IMAGE);
        this.id = id;
    }
}
