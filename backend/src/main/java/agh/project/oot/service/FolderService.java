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

    public Flux<Folder> getFolders() {
        return folderRepository.findAll();
    }

    public Mono<Folder> findById(Long id) {
        return folderRepository.findById(id);
    }

    public Mono<Long> getParentId(Long id) {
        return id == 0 ? Mono.just(0L) : findById(id).map(Folder::getParentId);
    }


    public Flux<Folder> getSubfolders(Long parentId) {
        return folderRepository.findByParentId(parentId);
    }

    public Mono<Long> createFolderIfNotExists(String folderName, Long parentId) {
        return folderRepository.findByNameAndParentId(folderName, parentId)
                .switchIfEmpty(folderRepository.save(new Folder(folderName, parentId)))
                .map(Folder::getId);
    }

    public Mono<Void> deleteFolderById(Long folderId) {
        return folderRepository.findById(folderId)
                .flatMap(folder -> folderRepository.deleteById(folderId))
                .then();
    }
}
