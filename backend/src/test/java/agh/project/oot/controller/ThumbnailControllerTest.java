package agh.project.oot.controller;

import agh.project.oot.messages.*;
import agh.project.oot.ResponseStatus;
import agh.project.oot.messages.DeleteImageMessage;
import agh.project.oot.messages.GetImageMessage;
import agh.project.oot.messages.GetNextPageMessage;
import agh.project.oot.messages.GetThumbnailsMessage;
import agh.project.oot.messages.PingMessage;
import agh.project.oot.messages.UploadImageMessage;
import agh.project.oot.messages.UploadZipMessage;
import agh.project.oot.model.IconDto;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import agh.project.oot.repository.ThumbnailRepository;
import agh.project.oot.utils.ImageReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static agh.project.oot.model.ThumbnailType.SMALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

//!TODO Fix
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ThumbnailControllerTest {
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ThumbnailRepository thumbnailRepository;

    @Autowired
    DatabaseClient databaseClient;

    @LocalServerPort
    private int port;

    private String URL;
    final List<Message> messagesList = new ArrayList<>();
    private TextWebSocketHandler handler;
    private volatile WebSocketSession testSession;
    private WebSocketClient client;
    private CountDownLatch latch;
    boolean await;

    @BeforeEach
    public void setup() {
        thumbnailRepository.deleteAll().block();
        deleteAllAndResetSequence().block();
        URL = "ws://localhost:" + port + "/upload-files";
        client = new StandardWebSocketClient();
        messagesList.clear();

        handler = new TextWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                testSession = session;
                testSession.setTextMessageSizeLimit(1024 * 1024);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
                Map<String, Object> messageMap = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
                MessageType type = MessageType.valueOf((String) messageMap.get("messageType"));
                Message responseMessage = switch (type) {
                    case UPLOAD_IMAGES ->
                            new UploadImageMessage(objectMapper.convertValue(messageMap.get("imagesData"), new TypeReference<>() {
                            }), PageRequest.of(Integer.parseInt((String) messageMap.get("page")), Integer.parseInt((String) messageMap.get("size"))),
                                    Long.valueOf((Integer) messageMap.get("folderId")));
                    case UPLOAD_ZIP ->
                            new UploadZipMessage(objectMapper.convertValue(messageMap.get("zipData"), new TypeReference<>() {
                            }), Long.valueOf((Integer) messageMap.get("folderId")));
                    case GET_THUMBNAILS ->
                            new GetThumbnailsMessage(ThumbnailType.valueOf((String) messageMap.get("thumbnailType")),
                                    PageRequest.of(Integer.parseInt((String) messageMap.get("page")), Integer.parseInt((String) messageMap.get("size"))),
                                    Long.valueOf((Integer) messageMap.get("folderId")));
                    case GET_IMAGE ->
                            new GetImageMessage(objectMapper.convertValue(messageMap.get("ids"), new TypeReference<>() {
                            }));
                    case DELETE_IMAGE ->
                            new DeleteImageMessage(objectMapper.convertValue(messageMap.get("id"), new TypeReference<>() {
                            }), Integer.parseInt((String) messageMap.get("size")));
                    case PONG ->
                            new PingMessage();
                    case GET_NEXT_PAGE ->
                            new GetNextPageMessage(PageRequest.of(Integer.parseInt((String) messageMap.get("page")),
                                    Integer.parseInt((String) messageMap.get("size"))), Long.valueOf((Integer) messageMap.get("folderId")));
                    case PLACEHOLDERS_NUMBER_RESPONSE, DELETE_IMAGE_RESPONSE, INFO_RESPONSE, FOLDERS_RESPONSE, FETCHING_END_RESPONSE, PING,
                         MOVE_IMAGE, MOVE_IMAGE_RESPONSE, DELETE_FOLDER, DELETE_FOLDER_RESPONSE ->
                            new InfoResponseMessage(ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                };
                messagesList.add(responseMessage);
                latch.countDown();
            }
        };
    }

    //TODO I don't know why only infoResponse and ping are sent
    @Test
    public void shouldReturnAllSaveThumbnailsAfterInitConnection() throws Exception {
        // Given
        Thumbnail linux = mockThumbnail("thumbnails/Linux.png");
        Thumbnail newYork = mockThumbnail("thumbnails/NewYork.png");
        Thumbnail ufo = mockThumbnail("thumbnails/Ufo.jpg");
        latch = new CountDownLatch(2);

        // When
        client.execute(handler, URL).get(5, TimeUnit.SECONDS);
        await = latch.await(20, TimeUnit.SECONDS);

        // Then
        assertTrue(await, "Too few messages");
        assertThat(messagesList).hasSize(5);

        assertThat(messagesList.getFirst()).isEqualTo(
                new InfoResponseMessage(ResponseStatus.OK, "Connection established")
        );

        assertThat(messagesList).contains(
                new GetNextPageMessage(PageRequest.of(1, 35), 0L)
        );

        assertThat(messagesList).contains(
                new GetThumbnailsMessage(SMALL, List.of(IconDto.from(linux)))
        );

        assertThat(messagesList).contains(
                new GetThumbnailsMessage(SMALL, List.of(IconDto.from(newYork)))
        );

        assertThat(messagesList).contains(
                new GetThumbnailsMessage(SMALL, List.of(IconDto.from(ufo)))
        );

        assertThat(messagesList).contains(
                new FetchingEndResponseMessage()
        );

        assertThat(messagesList).contains(
                new PingMessage()
        );
    }

    private Thumbnail mockThumbnail(String path) {
        byte[] image = ImageReader.loadImageAsBytes(path);
        Thumbnail thumbnail = new Thumbnail(image, SMALL);
        thumbnailRepository.save(thumbnail).block();
        return thumbnail;
    }

    public Mono<Void> deleteAllAndResetSequence() {
        return thumbnailRepository.deleteAll()
                .then(databaseClient.sql("ALTER SEQUENCE public.thumbnails_id_seq RESTART WITH 1")
                        .fetch()
                        .rowsUpdated()
                        .then());
    }

    //TODO Add test to check properly handling UnsupportedMediaType
}
