package agh.project.oot;

import agh.project.oot.model.IconDto;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Message {
    private ConnectionStatus connectionStatus;
    private ResponseStatus responseStatus;
    private MessageType type;
    private List<Long> ids = null;
    private String details = null;
    private List<IconDto> imagesData;
    private ThumbnailType thumbnailType = null;
    private Integer thumbnailsNumber = null;

    public Message() {
    }

    public Message(List<IconDto> imagesData, MessageType type, ThumbnailType thumbnailType) {
        this(ConnectionStatus.CONNECTED, ResponseStatus.OK, imagesData, thumbnailType, type);
    }

    public Message(MessageType type, Integer thumbnailsNumber) {
        this(ConnectionStatus.CONNECTED, ResponseStatus.OK, null, null, type, null, thumbnailsNumber);
    }

    public Message(ConnectionStatus connectionStatus, ResponseStatus responseStatus, List<IconDto> imagesData,
                   ThumbnailType thumbnailType, MessageType type) {
        this(connectionStatus, responseStatus, imagesData, thumbnailType, type, null, null);
    }

    public Message(ConnectionStatus connectionStatus, ResponseStatus responseStatus, List<IconDto> imagesData,
                   ThumbnailType thumbnailType, MessageType type, String details, Integer thumbnailsNumber) {
        this.responseStatus = responseStatus;
        this.connectionStatus = connectionStatus;
        this.type = type;
        this.imagesData = imagesData;
        this.thumbnailType = thumbnailType;
        this.details = details;
        this.thumbnailsNumber = thumbnailsNumber;
    }

    /**
     * Method that calculates the size of the message.
     * @return the size of the message in bytes.
     */
    public int calculateSize() {
        int size = 0;

        // Size of the connectionStatus field (enum)
        size += Integer.BYTES; // enum takes 4 bytes

        // Size of the responseStatus field (enum)
        size += Integer.BYTES; // enum takes 4 bytes

        // Size of the type field (enum)
        size += Integer.BYTES; // enum takes 4 bytes

        // Size of the imagesData field (List<IconDto>)
        if (imagesData != null) {
            size += Integer.BYTES; // for the list itself (reference)
            for (IconDto iconDto : imagesData) {
                size += calculateIconDtoSize(iconDto); // calculate the size of each IconDto
            }
        }

        // Size of the ids field (List<Long>)
        if (ids != null) {
            size += Integer.BYTES; // for the list itself (reference)
            for (Long ignored : ids) {
                size += Long.BYTES; // Long takes 8 bytes
            }
        }

        // Size of the details field (String)
        if (details != null) {
            size += details.getBytes(StandardCharsets.UTF_8).length; // calculate the size of the text in bytes
        }

        return size;
    }

    /**
     * Helper method that calculates the size of an IconDto object.
     * @param iconDto the IconDto object.
     * @return the size of the IconDto object in bytes.
     */
    private int calculateIconDtoSize(IconDto iconDto) {
        int size = 0;

        // Size of the id field (Long)
        size += Long.BYTES; // Long takes 8 bytes

        // Size of the data field (byte[])
        if (iconDto.getData() != null) {
            size += iconDto.getData().length; // size of the byte[] array (size of the data in bytes)
        }

        return size;
    }
}