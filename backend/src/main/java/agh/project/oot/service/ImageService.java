package agh.project.oot.service;

import agh.project.oot.ImageSink;
import agh.project.oot.model.Image;
import agh.project.oot.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageSink imageSink;

    public Mono<Image> saveAndNotifyThumbnail(Image image) {
        return imageRepository.save(image)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(image1 -> {
                        synchronized (imageSink){
                            imageSink.getSink().emitNext(image1.getId(), Sinks.EmitFailureHandler.FAIL_FAST);
                        }
                })
                .doOnError(error -> log.error("Error saving image with notification: {}", error.getMessage()));
    }

    public Mono<Image> findById(Long id) {
        return imageRepository.findById(id)
                .doOnError(error -> log.error("Error finding image by id: {}. [{}]", id, error.getMessage()));
    }

    public Flux<Image> findAllImages() {
        return imageRepository.findAll()
                .doOnError(error -> log.error("Error retrieving images from the database", error));
    }

    public Mono<Void> removeById(Long id) {
        return imageRepository.deleteById(id)
                .doOnError(error -> log.error("Error removing image from database", error));
    }

    public Mono<Boolean> updateImageOrder(Image image, long imageOrder) {
        image.setImageOrder(imageOrder);
        return imageRepository.updateImageOrder(image.getId(), imageOrder)
                .map(rowsUpdated -> rowsUpdated > 0)
                .doOnError(error -> log.error("Error updating image order for imageId: {}", image.getId(), error));
    }

    public Mono<Long> getImageOrderById(Long id) {
        return findById(id).map(Image::getImageOrder);
    }

    public Mono<Integer> getTopImageOrder() {
        return imageRepository.getTopByOrderByImageOrderDesc();
    }
}
