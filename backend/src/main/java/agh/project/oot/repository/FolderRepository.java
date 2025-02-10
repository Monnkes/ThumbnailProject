package agh.project.oot.repository;

import agh.project.oot.model.Folder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface FolderRepository extends ReactiveCrudRepository<Folder, Long> {
    Flux<Folder> findByParentId(Long parentId);
    Mono<Folder> findByNameAndParentId(String name, Long parentId);
}
