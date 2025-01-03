package agh.project.oot.service;

import agh.project.oot.model.Image;
import agh.project.oot.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    private final ImageRepository imageRepository;
    private final Sinks.Many<Long> imageSink;

    public Mono<Image> saveAndNotifyThumbnail(Image image) {
        return imageRepository.save(image)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(image1 -> {
                    Mono.defer(() -> {
                        synchronized (imageSink){
                            imageSink.emitNext(image1.getId(), Sinks.EmitFailureHandler.FAIL_FAST);
                        }
                        return Mono.empty();
                    }).subscribe();
                })
                .doOnError(error -> log.error("Error saving image with notification: {}", error.getMessage()));
    }

    public Mono<Image> findById(Long id) {
        return imageRepository.findById(id)
                .doOnError(error -> log.error("Error finding image by id: {}. [{}]", id, error.getMessage()));
    }
}
