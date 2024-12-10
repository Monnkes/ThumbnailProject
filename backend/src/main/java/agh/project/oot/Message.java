package agh.project.oot;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Message {
    private MessageType type;
    private List<byte[]> imagesData;
    private List<Integer> ids = null;

    public Message() {
    }

    public Message(List<byte[]> imagesData, MessageType type) {
        this.type = type;
        this.imagesData = imagesData;
    }
}
