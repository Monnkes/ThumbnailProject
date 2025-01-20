package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class FoldersResponseMessage extends Message {
    private List<Long> folderIds;
    private Long currentId;
    private Long parentId;

    public FoldersResponseMessage(List<Long> folderIds, Long currentId, Long parentId) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.FOLDERS_RESPONSE);
        this.folderIds = folderIds;
        this.currentId = currentId;
        this.parentId = parentId;
    }
}
