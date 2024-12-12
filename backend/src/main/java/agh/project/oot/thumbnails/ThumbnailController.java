package agh.project.oot.thumbnails;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.Message;
import agh.project.oot.MessageType;
import agh.project.oot.ResponseStatus;
import agh.project.oot.database.Thumbnail;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class ThumbnailController extends AbstractWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ThumbnailService thumbnailService;

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            processMessage(session, textMessage)
                    .subscribe(
                            success -> log.info("Message processed successfully"),
                            error -> log.error("Error processing message: {}", error.getMessage())
                    );
        }
    }

    private Mono<Void> processMessage(WebSocketSession session, TextMessage textMessage) {
        return Mono.fromCallable(() -> objectMapper.readValue(textMessage.getPayload(), Message.class))
                .flatMap(requestMessage -> {
                    MessageType messageType = requestMessage.getType();

                    return switch (messageType) {
                        case UploadImages -> handleUploadImages(session, requestMessage);
                        case GetImages -> handleGetThumbnails(session, requestMessage);
                        default ->
                                sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.BAD_REQUEST, null, MessageType.InfoResponse));
                    };
                })
                .onErrorResume(error -> sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.BAD_REQUEST,
                        null, MessageType.InfoResponse, error.getMessage())
                ));
    }

    private Mono<Void> handleUploadImages(WebSocketSession session, Message requestMessage) {
        List<byte[]> images = requestMessage.getImagesData();
        int thumbnailWidth = 100;
        int thumbnailHeight = 100;

        return thumbnailService.saveImages(images, thumbnailWidth, thumbnailHeight)
                .flatMap(savedIds -> {
                    log.info(savedIds.toString());
                    return sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.OK, null,
                            MessageType.InfoResponse, "Images uploaded successfully with IDs: " + savedIds));
                });
    }



    private Mono<Void> handleGetThumbnails(WebSocketSession session, Message requestMessage) {
        return thumbnailService.getAllThumbnails()
                .flatMap(picture -> {
                    Thumbnail thumbnail = picture.getThumbnail();
                    if (thumbnail == null) {
                        return Mono.empty();
                    }

                    byte[] thumbnailData = thumbnail.getData();
                    log.info("handleGetThumbnails");
                    return sendMessage(session, new Message(Collections.singletonList(thumbnailData), MessageType.GetImagesResponse));
                })
                .then();
    }

    private Mono<Void> sendMessage(WebSocketSession session, Message message) {
        return Mono.fromRunnable(() -> {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                log.error("Error sending message: {}", e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage("Connection established"));
        log.info("Connection established");
    }
}
