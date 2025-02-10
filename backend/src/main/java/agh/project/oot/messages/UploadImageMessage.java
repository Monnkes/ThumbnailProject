package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Setter
public final class UploadImageMessage extends Message {
    private List<IconDto> imagesData;
    private Pageable pageable;
    private Long folderId;

    public UploadImageMessage(List<IconDto> imagesData, Pageable pageable, Long folderId) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.UPLOAD_IMAGES);
        this.imagesData = imagesData;
        this.pageable = pageable;
        this.folderId = folderId;
    }
}
