package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class InfoResponseMessage extends Message {
    private ResponseStatus responseStatus;
    private String info;

    public InfoResponseMessage(ResponseStatus responseStatus) {
        this(responseStatus, null);
    }

    public InfoResponseMessage(ResponseStatus responseStatus, String info) {
        super(ConnectionStatus.CONNECTED, responseStatus, MessageType.INFO_RESPONSE);
        this.responseStatus = responseStatus;
        this.info = info;
    }
}
