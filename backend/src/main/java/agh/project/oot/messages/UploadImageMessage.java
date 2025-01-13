package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class UploadImageMessage extends Message {
    private List<IconDto> imagesData;

    public UploadImageMessage(List<IconDto> imagesData) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.UPLOAD_IMAGES);
        this.imagesData = imagesData;
    }
}
