package agh.project.oot.repository;

import agh.project.oot.model.Image;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ImageRepository extends ReactiveCrudRepository<Image, Long> {
    @Modifying
    @Query("UPDATE images SET image_order = :imageOrder WHERE id = :imageId")
    Mono<Integer> updateImageOrder(Long imageId, Long imageOrder);

    @Modifying
    @Query("UPDATE images SET folder_id = :folderId WHERE id = :id")
    Mono<Integer> updateFolderId(Long id, Long folderId);

    Mono<Image> findTopByFolderIdOrderByImageOrderDesc(Long folderId);

    Flux<Image> findByFolderIdOrderByImageOrder(Long folderId);

    Mono<Long> countByFolderId(Long folderId);
}
