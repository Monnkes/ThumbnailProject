package agh.project.oot.service;

import agh.project.oot.ResponseStatus;
import agh.project.oot.SessionData;
import agh.project.oot.SessionRepository;
import agh.project.oot.messages.FoldersResponseMessage;
import agh.project.oot.messages.GetThumbnailsMessage;
import agh.project.oot.messages.Message;
import agh.project.oot.messages.InfoResponseMessage;
import agh.project.oot.model.Folder;
import agh.project.oot.model.IconDto;
import agh.project.oot.thumbnails.UnsupportedImageFormatException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {
    private final ThumbnailService thumbnailService;
    private final SessionRepository sessionRepository;
    private final FolderService folderService;
    private final ObjectMapper objectMapper;

    @Value("${controller.maxAttempts}")
    private int maxAttempts;

    @Value("${controller.minBackoff}")
    private int minBackoff;

    @Value("${controller.delay}")
    private int delay;

    public Mono<Void> sendMessage(WebSocketSession session, Message message) {
        return Mono.defer(() -> {
            if (!session.isOpen()) {
                log.warn("Session is not open. Unable to send message. Session ID: {}", session.getId());
                return Mono.empty();
            }
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.info("Message sent successfully. Session ID: {}", session.getId());
                return Mono.empty();
            } catch (IOException e) {
                log.error("IOException while sending message. Session ID: {}, Error: {}", session.getId(), e.getMessage());
                return Mono.error(e);
            }
        }).retryWhen(getRetrySpec()).then();
    }

    public Mono<Void> sendFoldersForAll(Long folderId) {
        return folderService.getSubfolders(folderId)
                .sort()
                .collectList()
                .flatMap(folders ->
                        Flux.fromIterable(sessionRepository.getSessions().values())
                                .filter(sessionData -> sessionData.getFolderId().equals(folderId))
                                .flatMap(sessionData -> sendFolderResponseMessage(folderId, folders, sessionData))
                                .then()
                );
    }

    private Mono<Void> sendFolderResponseMessage(Long folderId, List<Folder> folders, SessionData sessionData) {
        return folderService.getParentId(folderId)
                .flatMap(parentId -> sendMessage(sessionData.getSession(),
                        new FoldersResponseMessage(folders, folderId, parentId)));
    }

    public Mono<Void> sendDeleteResponseMessage(Integer message) {
        return Flux.fromIterable(sessionRepository.getSessions().values())
                .flatMap(sessionData -> {
                    var type = sessionData.getThumbnailType();
                    var page = sessionData.getPageNumber();
                    var folder = sessionData.getFolderId();

                    return thumbnailService.findAllThumbnailsAfterDeleting(type, page, message, folder)
                            .flatMap(thumbnail -> sendMessage(
                                    sessionData.getSession(),
                                    new GetThumbnailsMessage(type, Collections.singletonList(IconDto.from(thumbnail))))
                            );
                })
                .then();
    }

    public Mono<Void> sendDeleteMessageResponse(Message message) {
        return Flux.fromIterable(sessionRepository.getSessions().values())
                .map(SessionData::getSession)
                .flatMap(session -> sendMessage(session, message))
                .doOnError(error -> log.error("Failed to send delete message response: {}", error.getMessage()))
                .then();
    }

    public Mono<Void> sendPingWithDelay(WebSocketSession session, Message message) {
        return Mono.delay(Duration.ofSeconds(delay))
                .then(sendMessage(session, message));
    }

    public Mono<Void> sendBadRequest(WebSocketSession session, String errorMessage, ResponseStatus responseStatus) {
        return sendMessage(session, new InfoResponseMessage(responseStatus, errorMessage));
    }

    public Mono<Void> sendErrorResponse(WebSocketSession session, Throwable error) {
        log.error(error.getMessage());

        ResponseStatus status = error instanceof UnsupportedImageFormatException
                ? ResponseStatus.UNSUPPORTED_MEDIA_TYPE
                : ResponseStatus.BAD_REQUEST;

        String message = "Internal error: " + error.getMessage();

        return sendMessage(session, new InfoResponseMessage(status, message));
    }

    private Retry getRetrySpec() {
        return Retry.backoff(maxAttempts, Duration.ofSeconds(minBackoff))
                .doBeforeRetry(retrySignal -> log.info("Retrying send attempt #{}", retrySignal.totalRetries() + 1))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                        new RuntimeException("Retries exhausted", retrySignal.failure())
                );
    }
}
