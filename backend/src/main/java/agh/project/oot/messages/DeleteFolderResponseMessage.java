package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class DeleteFolderResponseMessage extends Message {
    private Long id;

    public DeleteFolderResponseMessage(Long id) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.DELETE_FOLDER_RESPONSE);
        this.id = id;
    }
}
