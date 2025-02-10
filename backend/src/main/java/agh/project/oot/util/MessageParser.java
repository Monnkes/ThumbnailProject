package agh.project.oot.util;

import agh.project.oot.ResponseStatus;
import agh.project.oot.messages.*;
import agh.project.oot.model.ThumbnailType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageParser {
    private final ObjectMapper objectMapper;

    public Mono<Message> parseMessage(String payload) {
        return Mono.fromCallable(() -> {
                    Map<String, Object> messageMap = objectMapper.readValue(payload, new TypeReference<>() {});
                    MessageType type = MessageType.valueOf((String) messageMap.get("type"));
                    return parseByType(type, messageMap);
                })
                .onErrorResume(error -> {
                    log.error("Error parsing message", error);
                    return Mono.error(new IllegalArgumentException("Invalid message format", error));
                });
    }

    private Message parseByType(MessageType type, Map<String, Object> messageMap) {
        return switch (type) {
            case UPLOAD_IMAGES -> parseUploadImages(messageMap);
            case UPLOAD_ZIP -> parseUploadZip(messageMap);
            case GET_THUMBNAILS -> parseGetThumbnails(messageMap);
            case GET_IMAGE -> parseGetImage(messageMap);
            case MOVE_IMAGE -> parseMoveImage(messageMap);
            case DELETE_IMAGE -> parseDeleteImage(messageMap);
            case DELETE_FOLDER -> parseDeleteFolder(messageMap);
            case PONG -> new PingMessage();
            case GET_NEXT_PAGE -> parseGetNextPage(messageMap);
            case PLACEHOLDERS_NUMBER_RESPONSE,
                 DELETE_IMAGE_RESPONSE,
                 INFO_RESPONSE,
                 FOLDERS_RESPONSE,
                 DELETE_FOLDER_RESPONSE,
                 MOVE_IMAGE_RESPONSE,
                 FETCHING_END_RESPONSE,
                 PING -> new InfoResponseMessage(ResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        };
    }

    private UploadImageMessage parseUploadImages(Map<String, Object> messageMap) {
        return new UploadImageMessage(
                objectMapper.convertValue(messageMap.get("imagesData"), new TypeReference<>() {}),
                PageRequest.of(
                        Integer.parseInt((String) messageMap.get("page")),
                        Integer.parseInt((String) messageMap.get("size"))
                ),
                Long.valueOf((Integer) messageMap.get("folderId"))
        );
    }

    private UploadZipMessage parseUploadZip(Map<String, Object> messageMap) {
        return new UploadZipMessage(
                objectMapper.convertValue(messageMap.get("zipData"), new TypeReference<>() {}),
                Long.valueOf((Integer) messageMap.get("folderId"))
        );
    }

    private GetThumbnailsMessage parseGetThumbnails(Map<String, Object> messageMap) {
        return new GetThumbnailsMessage(
                ThumbnailType.valueOf((String) messageMap.get("thumbnailType")),
                PageRequest.of(
                        Integer.parseInt((String) messageMap.get("page")),
                        Integer.parseInt((String) messageMap.get("size"))
                ),
                Long.valueOf((Integer) messageMap.get("folderId"))
        );
    }

    private GetImageMessage parseGetImage(Map<String, Object> messageMap) {
        return new GetImageMessage(
                objectMapper.convertValue(messageMap.get("ids"), new TypeReference<>() {})
        );
    }

    private MoveImageMessage parseMoveImage(Map<String, Object> messageMap) {
        Set<Long> imageIds = objectMapper.convertValue(messageMap.get("imageId"), new TypeReference<>() {});
        Long currentFolderId = objectMapper.convertValue(messageMap.get("currentFolderId"), new TypeReference<>() {});
        Long targetFolderId = objectMapper.convertValue(messageMap.get("targetFolderId"), new TypeReference<>() {});
        return new MoveImageMessage(imageIds, currentFolderId, targetFolderId);
    }

    private DeleteImageMessage parseDeleteImage(Map<String, Object> messageMap) {
        return new DeleteImageMessage(
                objectMapper.convertValue(messageMap.get("id"), new TypeReference<>() {}),
                Integer.parseInt((String) messageMap.get("size"))
        );
    }

    private DeleteFolderMessage parseDeleteFolder(Map<String, Object> messageMap) {
        return new DeleteFolderMessage(
                objectMapper.convertValue(messageMap.get("id"), new TypeReference<>() {}),
                Integer.parseInt((String) messageMap.get("pageSize"))
        );
    }

    private GetNextPageMessage parseGetNextPage(Map<String, Object> messageMap) {
        return new GetNextPageMessage(
                PageRequest.of(
                        Integer.parseInt((String) messageMap.get("page")),
                        Integer.parseInt((String) messageMap.get("size"))
                ),
                Long.valueOf((Integer) messageMap.get("folderId"))
        );
    }
}