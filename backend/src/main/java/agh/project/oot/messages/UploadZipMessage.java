package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class UploadZipMessage extends Message {
    private byte[] zipData;

    public UploadZipMessage(byte[] zipData) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.UPLOAD_ZIP);
        this.zipData = zipData;
    }

}
