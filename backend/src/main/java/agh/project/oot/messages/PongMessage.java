package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;

public final class PongMessage extends Message {
    public PongMessage() {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.PONG);
    }
}
