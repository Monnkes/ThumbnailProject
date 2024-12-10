package agh.project.oot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Component
public class MessageController extends AbstractWebSocketHandler {

    private final List<byte[]> images = new ArrayList<>();
    private final ObjectMapper objectMapper;

    public MessageController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (message instanceof TextMessage) {
            Flux.just((TextMessage) message)
                    .flatMap(msg -> processImage(session, msg))
                    .subscribe();
        }
    }

    private Mono<Void> processImage(WebSocketSession session, TextMessage textMessage) {
        return Mono.fromRunnable(() -> {
            try {
                String jsonRequest = textMessage.getPayload();
                Message requestMessage = objectMapper.readValue(jsonRequest, Message.class);
                MessageType messageType = requestMessage.getType();

                TextMessage responseMessage = switch (messageType) {
                    case UploadImages:
                        log.info("Uploading images");
                        images.addAll(requestMessage.getImagesData());
                        yield new TextMessage("Image saved!");
                    case GetImages:
                        log.info("Getting images");
                        List<byte[]> response = new ArrayList<>();
                        List<Integer> ids = requestMessage.getIds();
                        ids.forEach(id -> response.add(images.get(id)));
                        String jsonResponse = objectMapper.writeValueAsString(new Message(response, MessageType.GetImagesResponse));
                        yield new TextMessage(jsonResponse);
                    default:
                        yield new TextMessage("Unknown message type!");
                };
                session.sendMessage(responseMessage);
            } catch (IOException e) {
                try {
                    log.warn(e.getMessage());
                    session.sendMessage(new TextMessage("Error processing image"));
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage("Connection established"));
        log.info("Connection established");
    }

}
