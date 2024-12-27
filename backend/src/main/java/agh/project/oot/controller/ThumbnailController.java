package agh.project.oot.controller;

import agh.project.oot.ResponseStatus;
import agh.project.oot.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailController extends AbstractWebSocketHandler {
    private final MessageService messageService;

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
                .flatMap(request -> switch (request.getType()) {
                    case UPLOAD_IMAGES -> messageService.handleUploadImages(session, request);
                    case GET_ALL_THUMBNAILS -> messageService.handleGetAllThumbnails(session);
                    case GET_IMAGE -> messageService.handleGetImage(session, request);
                    case PONG -> messageService.sendPingWithDelay(session);
                    case GET_THUMBNAILS_RESPONSE, GET_IMAGE_RESPONSE, INFO_RESPONSE, PING ->
                            messageService.sendBadRequest(session, "Unknown message type", ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                })
                .onErrorResume(error -> messageService.sendErrorResponse(session, error));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        messageService.afterConnectionEstablished(session);

        log.info("Connection established");

        messageService.handleGetAllThumbnails(session)
                .doOnSubscribe(subscription -> log.info("Starting to process thumbnails for session: {}", session))
                .doOnSuccess(aVoid -> log.info("Thumbnails processing completed successfully for session: {}", session))
                .doOnError(error -> log.error("Error occurred during thumbnails processing for session: {}", session, error))
                .doFinally(signalType -> log.info("Processing thumbnails finished with signal: {} for session: {}", signalType, session))
                .subscribe(
                        success -> log.info("All initial thumbnails sent successfully"),
                        error -> log.error("Error getting thumbnails", error)
                );

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed. Session ID: {}, Status: {}", session.getId(), status);
        super.afterConnectionClosed(session, status);
    }
}
