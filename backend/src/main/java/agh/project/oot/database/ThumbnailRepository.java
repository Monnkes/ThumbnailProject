package agh.project.oot.database;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ThumbnailRepository extends ReactiveCrudRepository<Thumbnail, Long> {
}
