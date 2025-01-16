package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;

public final class PingMessage extends Message {
    public PingMessage() {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.PING);
    }
}
