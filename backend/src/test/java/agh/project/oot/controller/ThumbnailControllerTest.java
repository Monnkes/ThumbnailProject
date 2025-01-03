//package agh.project.oot.controller;
//
//import agh.project.oot.ConnectionStatus;
//import agh.project.oot.Message;
//import agh.project.oot.MessageType;
//import agh.project.oot.ResponseStatus;
//import agh.project.oot.model.IconDto;
//import agh.project.oot.model.Thumbnail;
//import agh.project.oot.repository.ThumbnailRepository;
//import agh.project.oot.utils.ImageReader;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.r2dbc.core.DatabaseClient;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.client.WebSocketClient;
//import org.springframework.web.socket.client.standard.StandardWebSocketClient;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//import reactor.core.publisher.Mono;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//public class ThumbnailControllerTest {
//    @Autowired
//    ObjectMapper objectMapper;
//
//    @Autowired
//    ThumbnailRepository thumbnailRepository;
//
//    @Autowired
//    DatabaseClient databaseClient;
//
//    @LocalServerPort
//    private int port;
//
//    private String URL;
//    final List<Message> messagesList = new ArrayList<>();
//    private TextWebSocketHandler handler;
//    private volatile WebSocketSession testSession;
//    private WebSocketClient client;
//    private CountDownLatch latch;
//    boolean await;
//
//    @BeforeEach
//    public void setup() {
//        thumbnailRepository.deleteAll().block();
//        deleteAllAndResetSequence().block();
//        URL = "ws://localhost:" + port + "/upload-files";
//        client = new StandardWebSocketClient();
//        messagesList.clear();
//
//        handler = new TextWebSocketHandler() {
//
//            @Override
//            public void afterConnectionEstablished(WebSocketSession session) {
//                testSession = session;
//                testSession.setTextMessageSizeLimit(1024 * 1024);
//            }
//
//            @Override
//            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
//                Message responseMessage = objectMapper.readValue(message.getPayload(), Message.class);
//                messagesList.add(responseMessage);
//                latch.countDown();
//            }
//        };
//    }
//
//    @Test
//    public void shouldReturnAllSaveThumbnailsAfterInitConnection() throws Exception {
//        // Given
//        Thumbnail linux = mockThumbnail("thumbnails/Linux.png");
//        Thumbnail newYork = mockThumbnail("thumbnails/NewYork.png");
//        Thumbnail ufo = mockThumbnail("thumbnails/Ufo.jpg");
//        latch = new CountDownLatch(5);
//
//        // When
//        client.execute(handler, URL).get(5, TimeUnit.SECONDS);
//        await = latch.await(20, TimeUnit.SECONDS);
//
//        // Then
//        assertTrue(await, "Too few messages");
//        assertThat(messagesList).hasSize(5);
//
//        assertThat(messagesList.getFirst()).isEqualTo(new Message(
//                ConnectionStatus.CONNECTED,
//                ResponseStatus.OK,
//                null,
//                MessageType.INFO_RESPONSE,
//                "Connection established"
//        ));
//
//        assertThat(messagesList).contains(new Message(
//                ConnectionStatus.CONNECTED,
//                ResponseStatus.OK,
//                List.of(new IconDto(linux.getId(), linux.getData())),
//                MessageType.GET_THUMBNAILS_RESPONSE,
//                null
//        ));
//
//        assertThat(messagesList).contains(new Message(
//                ConnectionStatus.CONNECTED,
//                ResponseStatus.OK,
//                List.of(new IconDto(newYork.getId(), newYork.getData())),
//                MessageType.GET_THUMBNAILS_RESPONSE,
//                null
//        ));
//
//        assertThat(messagesList).contains(new Message(
//                ConnectionStatus.CONNECTED,
//                ResponseStatus.OK,
//                List.of(new IconDto(ufo.getId(), ufo.getData())),
//                MessageType.GET_THUMBNAILS_RESPONSE,
//                null
//        ));
//
//        assertThat(messagesList).contains(new Message(
//                ConnectionStatus.CONNECTED,
//                ResponseStatus.OK,
//                null,
//                MessageType.PING,
//                null
//        ));
//    }
//
//    private Thumbnail mockThumbnail(String path) {
//        byte[] image = ImageReader.loadImageAsBytes(path);
//        Thumbnail thumbnail = new Thumbnail(image);
//        thumbnailRepository.save(thumbnail).block();
//        return thumbnail;
//    }
//
//    public Mono<Void> deleteAllAndResetSequence() {
//        return thumbnailRepository.deleteAll()
//                .then(databaseClient.sql("ALTER SEQUENCE public.thumbnails_id_seq RESTART WITH 1")
//                        .fetch()
//                        .rowsUpdated()
//                        .then());
//    }
//}
