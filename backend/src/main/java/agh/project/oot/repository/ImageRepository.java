package agh.project.oot.repository;

import agh.project.oot.model.Image;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends ReactiveCrudRepository<Image, Long> {
}
