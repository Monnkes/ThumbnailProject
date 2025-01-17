package agh.project.oot.service;

import agh.project.oot.model.Folder;
import agh.project.oot.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService {
    private final FolderRepository folderRepository;

    public Mono<Folder> createFolder(String name, Long parentId) {
        return folderRepository.findById(parentId)
                .flatMap(parent -> {
                    Folder folder = new Folder();
                    folder.setName(name);
                    folder.setParentId(parentId);
                    return folderRepository.save(folder);
                });
    }

    public Mono<Folder> getFolder(Long id) {
        return folderRepository.findById(id);
    }

    public Flux<Folder> getSubfolders(Long parentId) {
        return folderRepository.findByParentId(parentId);
    }

    public Mono<Long> createFolderIfNotExists(String folderName, Long parentId) {
        // Sprawdź, czy folder istnieje, jeśli nie, utwórz go i zwróć jego id
        return folderRepository.findByNameAndParentId(folderName, parentId)
                .switchIfEmpty(folderRepository.save(new Folder(folderName, parentId)))
                .map(Folder::getId);
    }
}
