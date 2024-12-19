package agh.project.oot;

import agh.project.oot.model.IconDto;
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
    private List<IconDto> imagesData;
    private List<Long> ids = null;
    private String details = null;

    public Message() {
    }

    public Message(List<IconDto> imagesData, MessageType type) {
        this(ConnectionStatus.CONNECTED, ResponseStatus.OK, imagesData, type);
    }

    public Message(ConnectionStatus connectionStatus, ResponseStatus responseStatus, List<IconDto> imagesData, MessageType type) {
        this(connectionStatus, responseStatus, imagesData, type, null);
    }

    public Message(ConnectionStatus connectionStatus, ResponseStatus responseStatus, List<IconDto> imagesData, MessageType type, String details) {
        this.responseStatus = responseStatus;
        this.connectionStatus = connectionStatus;
        this.type = type;
        this.imagesData = imagesData;
        this.details = details;
    }

    /**
     * Metoda obliczająca rozmiar wiadomości.
     * @return rozmiar wiadomości w bajtach.
     */
    public int calculateSize() {
        int size = 0;

        // Rozmiar pola connectionStatus (enum)
        size += Integer.BYTES; // enum zajmuje 4 bajty

        // Rozmiar pola responseStatus (enum)
        size += Integer.BYTES; // enum zajmuje 4 bajty

        // Rozmiar pola type (enum)
        size += Integer.BYTES; // enum zajmuje 4 bajty

        // Rozmiar pola imagesData (List<IconDto>)
        if (imagesData != null) {
            size += Integer.BYTES; // dla samej listy (referencja)
            for (IconDto iconDto : imagesData) {
                size += calculateIconDtoSize(iconDto); // obliczanie rozmiaru każdego IconDto
            }
        }

        // Rozmiar pola ids (List<Long>)
        if (ids != null) {
            size += Integer.BYTES; // dla samej listy (referencja)
            for (Long id : ids) {
                size += Long.BYTES; // Long zajmuje 8 bajtów
            }
        }

        // Rozmiar pola details (String)
        if (details != null) {
            size += details.getBytes(StandardCharsets.UTF_8).length; // obliczanie rozmiaru tekstu w bajtach
        }

        return size;
    }

    /**
     * Metoda pomocnicza obliczająca rozmiar obiektu IconDto.
     * @param iconDto obiekt IconDto.
     * @return rozmiar obiektu IconDto w bajtach.
     */
    private int calculateIconDtoSize(IconDto iconDto) {
        int size = 0;

        // Rozmiar pola id (Long)
        size += Long.BYTES; // Long zajmuje 8 bajtów

        // Rozmiar pola data (byte[])
        if (iconDto.getData() != null) {
            size += iconDto.getData().length; // rozmiar tablicy byte[] (rozmiar danych w bajtach)
        }

        return size;
    }
}
