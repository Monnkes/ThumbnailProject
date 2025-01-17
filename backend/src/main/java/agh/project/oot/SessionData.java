package agh.project.oot;

import agh.project.oot.model.ThumbnailType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
public class SessionData {
    private final WebSocketSession session;

    @Setter
    private ThumbnailType thumbnailType;
    @Setter
    private Long folderId = 0L;

    public SessionData(WebSocketSession session, ThumbnailType thumbnailType) {
        this.session = session;
        this.thumbnailType = thumbnailType;
    }

    @Override
    public String toString() {
        return "SessionData{" +
                "sessionId=" + session.getId() +
                ", thumbnailType=" + thumbnailType +
                '}';
    }
}
