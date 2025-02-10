package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;

public final class FetchingEndResponseMessage extends Message {

    public FetchingEndResponseMessage() {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.FETCHING_END_RESPONSE);
    }
}
