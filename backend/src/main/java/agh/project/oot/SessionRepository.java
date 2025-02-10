package agh.project.oot;

import agh.project.oot.model.ThumbnailType;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
public class SessionRepository {
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session, ThumbnailType thumbnailType, Integer pageNumber) {
        sessions.put(session.getId(), new SessionData(session, thumbnailType, pageNumber));
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public SessionData getSessionById(String sessionId) {
        return sessions.get(sessionId);
    }

    public void updateSessionPageNumber(String sessionId, int pageNumber) {
        getSessions().computeIfPresent(sessionId, (key, sessionData) -> {
            sessionData.setPageNumber(pageNumber);
            return sessionData;
        });
    }
}

