package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import agh.project.oot.model.ThumbnailType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class GetThumbnailsMessage extends Message {
    private List<IconDto> imagesData;
    private ThumbnailType thumbnailType;
    private Long folderId;

    public GetThumbnailsMessage(ThumbnailType thumbnailType) {
        this(thumbnailType, null);
    }

    public GetThumbnailsMessage(ThumbnailType thumbnailType, List<IconDto> imagesData) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.GET_THUMBNAILS);
        this.thumbnailType = thumbnailType;
        this.imagesData = imagesData;
    }
}
