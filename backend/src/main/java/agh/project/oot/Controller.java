package agh.project.oot;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Deprecated
public class Controller {
    private final List<byte[]> images = new ArrayList<>();

    @GetMapping()
    public Mono<String> uploadFiles() {
        return Mono.just("OK");
    }

    @PostMapping(path = "/upload-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadFiles(@RequestPart("files") Flux<FilePart> fileParts) {
        return fileParts.flatMap(filePart ->
                        filePart.content()
                                .map(dataBuffer -> {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    DataBufferUtils.release(dataBuffer);
                                    return bytes;
                                })
                                .collectList()
                                .map(content -> {
                                    byte[] fileContent = new byte[content.stream().mapToInt(b -> b.length).sum()];
                                    int offset = 0;
                                    for (byte[] chunk : content) {
                                        System.arraycopy(chunk, 0, fileContent, offset, chunk.length);
                                        offset += chunk.length;
                                    }
                                    images.add(fileContent);
                                    return "Plik " + filePart.filename() + " został zapisany!";
                                })
                ).collectList()
                .map(results -> "Wszystkie pliki zostały przetworzone!");
    }

    @GetMapping(path = "/images/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<ResponseEntity<byte[]>> getImage(@PathVariable int id) {
        if (id < 0 || id >= images.size()) {
            return Mono.error(new IllegalArgumentException("Invalid image ID"));
        }
        byte[] image = images.get(id);
        return Mono.just(ResponseEntity.ok().body(image));
    }


    @GetMapping(path = "/images", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<byte[]>>> getImages() {
        return Mono.just(ResponseEntity.ok().body(images));
    }
}
