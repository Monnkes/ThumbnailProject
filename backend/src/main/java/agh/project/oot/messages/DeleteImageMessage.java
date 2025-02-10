package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class DeleteImageMessage extends Message {
    private Long id;
    private Integer pageSize;

    public DeleteImageMessage(Long id, Integer pageSize) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.DELETE_IMAGE);
        this.id = id;
        this.pageSize = pageSize;
    }
}
