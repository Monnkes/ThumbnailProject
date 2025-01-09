package agh.project.oot.repository;

import agh.project.oot.model.Thumbnail;
import agh.project.oot.model.ThumbnailType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ThumbnailRepository extends ReactiveCrudRepository<Thumbnail, Long> {
    Flux<Thumbnail> findByType(ThumbnailType type);
    Mono<Thumbnail> findByImageIdAndType(Long imageId, ThumbnailType type);
}
