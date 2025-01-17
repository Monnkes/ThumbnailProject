package agh.project.oot.controller;

import agh.project.oot.ResponseStatus;
import agh.project.oot.SessionRepository;
import agh.project.oot.messages.*;
import agh.project.oot.service.FolderService;
import agh.project.oot.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.publisher.Mono;

import static agh.project.oot.model.ThumbnailType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailController extends AbstractWebSocketHandler {
    @Lazy
    private final MessageService messageService;
    private final SessionRepository sessionRepository;

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (message instanceof TextMessage textMessage) {
            processMessage(session, textMessage)
                    .subscribe(
                            success -> log.info("Message processed successfully"),
                            error -> log.error("Error processing message: {}", error.getMessage())
                    );
        }
    }

    private Mono<Void> processMessage(WebSocketSession session, TextMessage textMessage) {
        return messageService.parseMessage(textMessage.getPayload())
                .flatMap(message -> switch (message) {
                    case UploadImageMessage uploadImageMessage -> messageService.handleUploadImages(uploadImageMessage);
                    case UploadZipMessage uploadZipMessage -> messageService.handleUploadZip(uploadZipMessage);
                    case GetThumbnailsMessage getThumbnailsMessage -> setThumbnailTypeAndResponse(session, getThumbnailsMessage);
                    case GetImageMessage getImageMessage -> messageService.handleGetImage(session, getImageMessage);
                    case PingMessage pingMessage -> messageService.sendPingWithDelay(session, pingMessage);
                    case InfoResponseMessage infoResponseMessage ->
                            messageService.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    //Temporary
                    default -> throw new IllegalStateException("Unexpected value: " + message);
                })
                .onErrorResume(error -> messageService.sendErrorResponse(session, error));
    }

    private Mono<Void> setThumbnailTypeAndResponse(WebSocketSession session, GetThumbnailsMessage message) {
        sessionRepository.getSessions().computeIfPresent(session.getId(), (key, sessionData) -> {
            sessionData.setThumbnailType(message.getThumbnailType());
            return sessionData;
        });
        return messageService.handleGetAllThumbnails(session, message);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        messageService.afterConnectionEstablished(session);
        sessionRepository.addSession(session, SMALL);

        log.info("Connection established; session id:{}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed. Session ID: {}, Status: {}", session.getId(), status);
        sessionRepository.remove(session);
        super.afterConnectionClosed(session, status);
    }
}
