package agh.project.oot.controller;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.Message;
import agh.project.oot.MessageType;
import agh.project.oot.ResponseStatus;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.repository.ThumbnailRepository;
import agh.project.oot.utils.ImageReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ThumbnailControllerTest {
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ThumbnailRepository thumbnailRepository;

    @LocalServerPort
    private int port;

    private String URL;
    final List<Message> messagesList = new ArrayList<>();
    private TextWebSocketHandler handler;
    private volatile WebSocketSession testSession;
    private WebSocketClient client;
    private CountDownLatch latch;

    @BeforeEach
    public void setup() {
        URL = "ws://localhost:" + port + "/upload-files";
        client = new StandardWebSocketClient();
        latch = new CountDownLatch(10);
        messagesList.clear();
        System.err.println(port);

        handler = new TextWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                testSession = session;
                testSession.setTextMessageSizeLimit(1024 * 1024 * 10);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
                Message responseMessage = objectMapper.readValue(message.getPayload(), Message.class);
                messagesList.add(responseMessage);
                System.err.println("ID" + session.getId() + " " + responseMessage);
                latch.countDown();
            }
        };
    }

    @Test
    public void testThumbnailController() throws Exception {
//        mockThumbnail("thumbnails/Linux.png");
//        mockThumbnail("thumbnails/NewYork.png");
//        mockThumbnail("thumbnails/Ufo.jpg");
//        client.execute(handler, URL).get(30, TimeUnit.SECONDS);
//
//        boolean await = latch.await(30, TimeUnit.SECONDS);
////        assertTrue(await, "Nie otrzymano odpowiedzi");
//
//        System.err.println(messagesList);
//
//        boolean awaitResponse = latch.await(20, TimeUnit.SECONDS);
////        assertTrue(awaitResponse, "Nie otrzymano odpowiedzi");
//        testSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Message(null, MessageType.PONG))));
//
//        System.err.println(messagesList);
//
//        boolean a = latch.await(20, TimeUnit.SECONDS);
//        testSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Message(null, MessageType.PONG))));
//
//        boolean b = latch.await(20, TimeUnit.SECONDS);
//        testSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Message(null, MessageType.PONG))));
//
//        assertNotNull(messagesList.getFirst(), "Odpowiedź jest pusta");
//        assertThat(messagesList.getFirst()).isEqualTo(new Message(ConnectionStatus.CONNECTED, ResponseStatus.OK, null, MessageType.INFO_RESPONSE, "Connection established"));
//        System.err.println(messagesList);
//
//        assertThat(messagesList.getFirst()).isEqualTo(new Message(ConnectionStatus.CONNECTED, ResponseStatus.OK, null, MessageType.INFO_RESPONSE, "Connection established"));
    }

    private void mockThumbnail(String path) {
        byte[] image = ImageReader.loadImageAsBytes(path);
        Thumbnail thumbnail =  new Thumbnail(image);
        thumbnailRepository.save(thumbnail).block();
    }
}
