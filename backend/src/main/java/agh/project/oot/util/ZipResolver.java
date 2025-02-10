package agh.project.oot.util;

import agh.project.oot.model.Image;
import agh.project.oot.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
public class ZipResolver {
    private final FolderService folderService;

    public Flux<Image> processZipEntries(ZipInputStream zis) {
        return Flux.create(sink -> {
            try {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        processZipEntry(zis, entry, sink);
                    }
                }
                sink.complete();
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    private void processZipEntry(ZipInputStream zis, ZipEntry entry, FluxSink<Image> sink) throws IOException {
        byte[] fileBytes = zis.readAllBytes();
        String fullPath = entry.getName();
        List<String> folderPath = extractFolderPath(fullPath);

        createFolders(folderPath)
                .map(finalFolderId -> {
                    Image image = new Image(fileBytes);
                    image.setFolderId(finalFolderId);
                    return sink.next(image);
                })
                .block();
    }

    private Mono<Long> createFolders(List<String> folderPath) {
        return Flux.fromIterable(folderPath)
                .reduce(Mono.just(0L), (parentMono, folderName) ->
                        parentMono.flatMap(parentId -> folderService.createFolderIfNotExists(folderName, parentId))
                )
                .flatMap(finalId -> finalId);
    }

    private List<String> extractFolderPath(String fullPath) {
        List<String> parts = List.of(fullPath.split("/"));
        return parts.subList(0, parts.size() - 1);
    }
}
