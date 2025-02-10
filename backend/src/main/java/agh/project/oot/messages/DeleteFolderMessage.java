package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class DeleteFolderMessage extends Message {
    private Long id;
    private Integer pageSize;

    public DeleteFolderMessage(Long id, Integer pageSize) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.DELETE_FOLDER);
        this.id = id;
        this.pageSize = pageSize;
    }
}
