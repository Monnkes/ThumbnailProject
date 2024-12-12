package agh.project.oot.database;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThumbnailRepository extends ReactiveCrudRepository<Thumbnail, Long> {
}
