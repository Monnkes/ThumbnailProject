package agh.project.oot;

import agh.project.oot.model.ThumbnailType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Setter
public class SessionData {
    private final WebSocketSession session;
    private ThumbnailType thumbnailType;
    private Long folderId = 0L;
    private Integer pageNumber;

    public SessionData(WebSocketSession session, ThumbnailType thumbnailType, Integer pageNumber) {
        this.session = session;
        this.thumbnailType = thumbnailType;
        this.pageNumber = pageNumber;
    }
}
