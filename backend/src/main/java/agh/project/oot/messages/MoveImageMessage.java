package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public final class MoveImageMessage extends Message {
    private Set<Long> imageIds;
    private Long currentFolderId;
    private Long targetFolderId;

    public MoveImageMessage(Set<Long> imageIds, Long currentFolderId, Long targetFolderId) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.MOVE_IMAGE);
        this.imageIds = imageIds;
        this.currentFolderId = currentFolderId;
        this.targetFolderId = targetFolderId;
    }
}
