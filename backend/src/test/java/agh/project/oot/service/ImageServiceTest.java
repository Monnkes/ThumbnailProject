package agh.project.oot.service;

import agh.project.oot.ImageSink;
import agh.project.oot.model.Image;
import agh.project.oot.repository.ImageRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

//! TODO Fix it
@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ImageSink imageSink;

    @InjectMocks
    private ImageService imageService;

    @Test
    void shouldSaveAndNotifyThumbnail() {
        // Given
        Image image = new Image(new byte[]{1, 2, 3}, 1L);
        when(imageRepository.save(any(Image.class))).thenReturn(Mono.just(image));

        // When
        Mono<Image> result = imageService.saveAndNotifyThumbnail(image);

        // Then
        StepVerifier.create(result)
                .expectNext(image)
                .verifyComplete();

        // Verify interactions
        verify(imageRepository).save(any(Image.class));
        verify(imageSink.getSink()).emitNext(image.getId(), Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Test
    void shouldHandleErrorOnSaveAndNotifyThumbnail() {
        // Given
        Image image = new Image(new byte[]{1, 2, 3}, 1L);
        when(imageRepository.save(any(Image.class))).thenReturn(Mono.error(new RuntimeException("Error saving image")));

        // When
        Mono<Image> result = imageService.saveAndNotifyThumbnail(image);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Error saving image"))
                .verify();

        // Verify interactions
        verify(imageRepository).save(any(Image.class));
        verify(imageSink.getSink(), Mockito.never()).emitNext(any(), any());
    }

    @Test
    void shouldFindImageById() {
        // Given
        Image image = new Image(new byte[]{1, 2, 3}, 1L);
        when(imageRepository.findById(1L)).thenReturn(Mono.just(image));

        // When
        Mono<Image> result = imageService.findById(1L);

        // Then
        StepVerifier.create(result)
                .expectNext(image)
                .verifyComplete();

        // Verify interactions
        verify(imageRepository).findById(1L);
    }

    @Test
    void shouldHandleErrorOnFindImageById() {
        // Given
        when(imageRepository.findById(1L)).thenReturn(Mono.error(new RuntimeException("Error finding image")));

        // When
        Mono<Image> result = imageService.findById(1L);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Error finding image"))
                .verify();

        // Verify interactions
        verify(imageRepository).findById(1L);
    }
}
