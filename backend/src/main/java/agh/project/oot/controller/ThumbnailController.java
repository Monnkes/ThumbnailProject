package agh.project.oot.controller;

import agh.project.oot.ResponseStatus;
import agh.project.oot.SessionRepository;
import agh.project.oot.messages.*;
import agh.project.oot.messages.DeleteFolderMessage;
import agh.project.oot.messages.DeleteFolderResponseMessage;
import agh.project.oot.messages.DeleteImageMessage;
import agh.project.oot.messages.DeleteImageResponseMessage;
import agh.project.oot.messages.GetImageMessage;
import agh.project.oot.messages.GetNextPageMessage;
import agh.project.oot.messages.GetThumbnailsMessage;
import agh.project.oot.messages.MoveImageMessage;
import agh.project.oot.messages.MoveImageResponseMessage;
import agh.project.oot.messages.PingMessage;
import agh.project.oot.messages.PongMessage;
import agh.project.oot.messages.UploadImageMessage;
import agh.project.oot.messages.UploadZipMessage;
import agh.project.oot.util.MessageParser;
import agh.project.oot.service.MessageSender;
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
    private final MessageSender messageSender;
    private final MessageParser messageParser;

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
        return messageParser.parseMessage(textMessage.getPayload())
                .flatMap(message -> switch (message) {
                    case UploadImageMessage uploadImageMessage -> messageService.handleUploadImages(uploadImageMessage);
                    case UploadZipMessage uploadZipMessage -> messageService.handleUploadZip(uploadZipMessage);
                    case GetThumbnailsMessage getThumbnailsMessage -> setThumbnailTypeAndResponse(session, getThumbnailsMessage);
                    case GetImageMessage getImageMessage -> messageService.handleGetImage(session, getImageMessage);
                    case MoveImageMessage moveImageMessage -> messageService.handleMoveImage(session, moveImageMessage);
                    case DeleteImageMessage deleteImageMessage -> messageService.handleDeleteImage(deleteImageMessage);
                    case DeleteFolderMessage deleteFolderMessage -> messageService.handleDeleteFolder(deleteFolderMessage);
                    case PingMessage pingMessage -> messageSender.sendPingWithDelay(session, pingMessage);
                    case GetNextPageMessage getNextPageMessage -> messageService.handleGetNextPage(session, getNextPageMessage);
                    case InfoResponseMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    case PongMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    case PlaceholderNumberMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    case FoldersResponseMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    case FetchingEndResponseMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    case MoveImageResponseMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    case DeleteFolderResponseMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                    case DeleteImageResponseMessage ignored -> messageSender.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                })
                .onErrorResume(error -> messageSender.sendErrorResponse(session, error));
    }

    private Mono<Void> setThumbnailTypeAndResponse(WebSocketSession session, GetThumbnailsMessage message) {
        sessionRepository.getSessions().computeIfPresent(session.getId(), (key, sessionData) -> {
            sessionData.setThumbnailType(message.getThumbnailType());
            sessionData.setPageNumber(message.getPageable().getPageNumber());
            return sessionData;
        });
        sessionRepository.getSessionById(session.getId()).setFolderId(message.getFolderId());

        return messageService.handleGetAllThumbnails(session, message);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        messageService.afterConnectionEstablished(session);
        sessionRepository.addSession(session, SMALL, 1);

        log.info("Connection established; session id:{}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed. Session ID: {}, Status: {}", session.getId(), status);
        sessionRepository.remove(session);
        super.afterConnectionClosed(session, status);
    }
}
