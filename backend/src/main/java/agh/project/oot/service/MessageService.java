package agh.project.oot.service;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.Message;
import agh.project.oot.MessageType;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.thumbnails.UnsupportedImageFormatException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageService {
    private final ObjectMapper objectMapper;
    private final ThumbnailService thumbnailService;

    @Value("${controller.maxAttempts}")
    private int maxAttempts;

    @Value("${controller.minBackoff}")
    private int minBackoff;

    @Value("${controller.delay}")
    private int delay;

    public Mono<Message> parseMessage(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, Message.class))
                .onErrorResume(error -> {
                    log.error("Error parsing message", error);
                    return Mono.error(new IllegalArgumentException("Invalid message format", error));
                });
    }

    public Mono<Void> sendMessage(WebSocketSession session, Message message) {
        log.info("Preparing to send message. Session ID: {}", session.getId());
        log.info("WebSocket message size: {} bytes", message.calculateSize());

        return Mono.defer(() -> {
                    if (!session.isOpen()) {
                        log.warn("Session is not open. Unable to send message. Session ID: {}", session.getId());
                        return Mono.empty();
                    }
                    try {
                        log.debug("Serializing message: {}", message);
                        String jsonMessage = objectMapper.writeValueAsString(message);

                        log.debug("Sending serialized message: {}", jsonMessage);
                        session.sendMessage(new TextMessage(jsonMessage));

                        log.info("Message sent successfully. Session ID: {}", session.getId());
                        return Mono.empty();
                    } catch (IOException e) {
                        log.error("IOException while sending message. Session ID: {}, Error: {}", session.getId(), e.getMessage());
                        return Mono.error(e);
                    } catch (Exception e) {
                        log.error("Unexpected error while sending message. Session ID: {}, Error: {}", session.getId(), e.getMessage());
                        return Mono.error(e);
                    }
                })
                .doOnSubscribe(subscription -> log.debug("Subscription started for sending message. Session ID: {}", session.getId()))
                .doOnError(e -> log.error("Error occurred during message sending. Session ID: {}, Error: {}", session.getId(), e.getMessage()))
                .doOnSuccess(ignored -> log.debug("Message sending process completed successfully. Session ID: {}", session.getId()))
                .retryWhen(getRetrySpec())
                .doFinally(signal -> log.info("Send message process finished with signal: {}. Session ID: {}", signal, session.getId()))
                .then();
    }


    public Mono<Void> handleUploadImages(WebSocketSession session, Message request) {
        List<Image> images = request.getImagesData().stream()
                .map(icon -> new Image(icon.getData()))
                .toList();

        return Flux.fromIterable(images)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(image -> this.processSingleImage(session, image))
                .sequential()
                .then();
    }

    private Flux<Thumbnail> processSingleImage(WebSocketSession session, Image image) {
        return thumbnailService.saveImageAndThumbnails(image)
                .flatMap(thumbnail ->
                        sendMessage(session, new Message(Collections.singletonList(IconDto.from(thumbnail)), MessageType.GET_THUMBNAILS_RESPONSE))
                                .then(Mono.just(thumbnail))
                )
                .onErrorResume(error -> {
                    log.error("Error processing image: {}", error.getMessage());
                    return sendErrorResponse(session, error)
                            .then(Mono.empty());
                });
    }

    public Mono<Void> handleGetAllThumbnails(WebSocketSession session, String type) {
        return thumbnailService.getAllThumbnails(type)
                .flatMap(thumbnail -> {
                    try {
                        return sendMessage(session, new Message(Collections.singletonList(IconDto.from(thumbnail)), MessageType.GET_THUMBNAILS_RESPONSE));
                    } catch (Exception e) {
                        log.error("Error while sending thumbnail message", e);
                        return Mono.empty();
                    }
                })
                .doOnComplete(() -> log.info("All initial thumbnails sent successfully"))
                .doOnError(error -> log.error("Error getting thumbnails", error))
                .onErrorResume(e -> {
                    log.error("Fallback due to error", e);
                    return Mono.error(e);
                })
                .then();
    }

    public Mono<Void> handleGetImage(WebSocketSession session, Message request) {
        Long imageId = request.getIds().getFirst();

        return thumbnailService.getImageByThumbnailId(imageId)
                .flatMap(imageData -> sendMessage(session, new Message(List.of(IconDto.from(imageData)), MessageType.GET_IMAGE_RESPONSE)))
                .then();
    }

    public Mono<Void> sendPingWithDelay(WebSocketSession session) {
        return Mono.delay(Duration.ofSeconds(delay))
                .then(sendMessage(session, new Message(null, MessageType.PING)));
    }

    public Mono<Void> sendBadRequest(WebSocketSession session, String errorMessage, ResponseStatus responseStatus) {
        return sendMessage(session, new Message(ConnectionStatus.CONNECTED, responseStatus, null, null, MessageType.INFO_RESPONSE, errorMessage));
    }

    public Mono<Void> sendErrorResponse(WebSocketSession session, Throwable error) {
        log.error("Error processing message {}", error.getMessage());
        if (error.getClass() == UnsupportedImageFormatException.class || error.getClass() == UnsupportedFormatException.class) {
            return sendBadRequest(session, "Internal error: " + error.getMessage(), ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return sendBadRequest(session, "Internal error: " + error.getMessage(), ResponseStatus.BAD_REQUEST);
    }

    public void afterConnectionEstablished(WebSocketSession session) {
        sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.OK, null, null, MessageType.INFO_RESPONSE, "Connection established"))
                .then(sendPingWithDelay(session))
                .subscribe(
                        success -> log.info("Initial connection setup completed"),
                        error -> log.error("Error during connection setup", error)
                );
    }

    private RetryBackoffSpec getRetrySpec() {
        return Retry.backoff(maxAttempts, Duration.ofSeconds(minBackoff))
                .doBeforeRetry(retrySignal -> log.info("Retrying send attempt #{}", retrySignal.totalRetries() + 1))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new RuntimeException("Retries exhausted", retrySignal.failure()));
    }
}
