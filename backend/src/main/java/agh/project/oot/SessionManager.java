package agh.project.oot;

import agh.project.oot.model.ThumbnailType;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
public class SessionManager {
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session, ThumbnailType thumbnailType) {
        sessions.put(session.getId(), new SessionData(session, thumbnailType));
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session.getId());
    }
}

