package agh.project.oot.repository;

import agh.project.oot.model.Image;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ImageRepository extends ReactiveCrudRepository<Image, Long> {
    // TODO Upgrade the query
    @Modifying
    @Query("UPDATE images SET image_order = :imageOrder WHERE id = :imageId")
    Mono<Integer> updateImageOrder(Long imageId, Long imageOrder);

    @Query("SELECT MAX(image_order) FROM postgres.public.images")
    Mono<Integer> getTopByOrderByImageOrderDesc();
}
