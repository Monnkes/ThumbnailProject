package agh.project.oot.repository;

import agh.project.oot.model.Thumbnail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import agh.project.oot.model.ThumbnailType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ThumbnailRepository extends ReactiveCrudRepository<Thumbnail, Long> {
    Flux<Thumbnail> findByType(ThumbnailType type);
    Mono<Thumbnail> findByImageIdAndType(Long imageId, ThumbnailType type);
    Flux<Thumbnail> findByImageId(Long imageId);
    Flux<Thumbnail> removeAllByImageId(Long imageId);

    @Query("""
    SELECT * FROM thumbnails t
    JOIN images i ON t.image_id = i.id
    WHERE t.type = :type AND i.folder_id = :folderId
    ORDER BY t.thumbnail_order
    LIMIT :limit OFFSET :offset
    """)
    Flux<Thumbnail> findByTypeAndFolderPaginated(
            @Param("type") ThumbnailType type,
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("folderId") int folderId
    );

    @Modifying
    @Query("UPDATE thumbnails SET thumbnail_order = :thumbnailOrder WHERE id = :thumbnailId")
    Mono<Integer> updateThumbnailOrder(Long thumbnailId, Long thumbnailOrder);
}