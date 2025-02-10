package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.Folder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class FoldersResponseMessage extends Message {
    private List<Folder> folders;
    private Long currentId;
    private Long parentId;
    private String folderName;

    public FoldersResponseMessage(List<Folder> folders, Long currentId, Long parentId) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.FOLDERS_RESPONSE);
        this.folders = folders;
        this.currentId = currentId;
        this.parentId = parentId;
    }
}
