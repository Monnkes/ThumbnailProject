package agh.project.oot.messages;

import agh.project.oot.ConnectionStatus;
import agh.project.oot.ResponseStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public final class GetNextPageMessage extends Message {

    private Pageable pageable;
    private Long folderId;

    public GetNextPageMessage(Pageable pageable, Long folderId) {
        super(ConnectionStatus.CONNECTED, ResponseStatus.OK, MessageType.GET_NEXT_PAGE);
        this.pageable = pageable;
        this.folderId = folderId;
    }
}
