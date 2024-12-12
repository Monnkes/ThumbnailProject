package agh.project.oot.thumbnails;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.Message;
import agh.project.oot.MessageType;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.IconDto;
import agh.project.oot.model.ImageDto;
import agh.project.oot.model.ThumbnailDto;
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
        return Mono.fromCallable(() -> objectMapper.readValue(textMessage.getPayload(), Message.class))
                .flatMap(requestMessage -> {
                    log.info("Message received: {}", requestMessage);
                    MessageType messageType = requestMessage.getType();

                    return switch (messageType) {
                        case UploadImages -> handleUploadImages(session, requestMessage);
                        case GetAllThumbnails -> handleGetAllThumbnails(session);
                        case GetImage -> handleGetImage(session, requestMessage);
                        default ->
                                sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.BAD_REQUEST, null, MessageType.InfoResponse));
                    };
                })
                .onErrorResume(error -> sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.BAD_REQUEST,
                        null, MessageType.InfoResponse, error.getMessage())
                ));
    }

    private Mono<Void> handleUploadImages(WebSocketSession session, Message requestMessage) {
        List<ImageDto> images = requestMessage.getImagesData().stream()
                .map(icon -> new ImageDto(icon.getData()))
                .toList();

        return thumbnailService.saveImagesAndSendThumbnails(images)
                .flatMap(thumbnailData -> sendMessage(session, new Message(Collections.singletonList(new IconDto(thumbnailData.getId(), thumbnailData.getData())), MessageType.GetThumbnailsResponse)))
                .then(sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.OK, null,
                        MessageType.InfoResponse, "Images uploaded and thumbnails sent successfully")));
    }

    private Mono<Void> handleGetAllThumbnails(WebSocketSession session) {
        return thumbnailService.getAllThumbnails()
                .flatMap(thumbnailData -> sendMessage(session, new Message(Collections.singletonList(new ThumbnailDto(thumbnailData.getData())), MessageType.GetThumbnailsResponse)))
                .then();
    }

    private Mono<Void> handleGetImage(WebSocketSession session, Message requestMessage) {
        Long imageId = requestMessage.getIds().getFirst();

        return thumbnailService.getImageById(imageId)
                .flatMap(imageData -> {
                    var img = new ImageDto(imageData.getData());
                    List<IconDto> temp = List.of(img);
                    var msg = new Message(List.copyOf(temp), MessageType.GetImageResponse);
                    msg.setImagesData(List.copyOf(temp));

                    return sendMessage(session, msg);
                })
                .then();
    }

    private Mono<Void> sendMessage(WebSocketSession session, Message message) {
        return Mono.fromRunnable(() -> {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.info("Message sent successfully");
            } catch (IOException e) {
                log.error("Error sending message: {}", e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sendMessage(session, new Message(ConnectionStatus.CONNECTED, ResponseStatus.OK, null, MessageType.InfoResponse, "Connection established"));
        log.info("Connection established");

        handleGetAllThumbnails(session)
                .then(Mono.defer(Mono::empty))
                .subscribe(
                        success -> log.info("All init thumbnails sent successfully"),
                        error -> log.error("Error getting thumbnails", error) // error callback
                );
    }
}
