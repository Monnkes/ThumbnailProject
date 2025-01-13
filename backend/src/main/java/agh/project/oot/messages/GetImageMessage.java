package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class GetImageMessage extends Message {
    // TODO Consider using Long instance List
    private List<Long> ids;
    private List<IconDto> imagesData;

    public GetImageMessage(List<Long> ids) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.GET_IMAGE);
        this.ids = ids;
    }
}
