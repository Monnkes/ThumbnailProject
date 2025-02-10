package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import agh.project.oot.model.ThumbnailType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Getter
@Setter
public final class GetThumbnailsMessage extends Message {
    private List<IconDto> imagesData;
    private ThumbnailType thumbnailType;
    private Long folderId;
    private Pageable pageable;

    public GetThumbnailsMessage(ThumbnailType thumbnailType, Pageable pageable, Long folderId) {
        this(thumbnailType, null, pageable, folderId);
    }

    public GetThumbnailsMessage(ThumbnailType thumbnailType, List<IconDto> imagesData) {
        this(thumbnailType, imagesData, null, null);
    }

    public GetThumbnailsMessage(ThumbnailType thumbnailType, List<IconDto> imagesData, Pageable pageable, Long folderId) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.GET_THUMBNAILS);
        this.thumbnailType = thumbnailType;
        this.imagesData = imagesData;
        this.pageable = pageable;
        this.folderId = folderId;
    }
}
