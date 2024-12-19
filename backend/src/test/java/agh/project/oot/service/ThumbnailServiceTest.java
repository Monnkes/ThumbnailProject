package agh.project.oot.service;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import agh.project.oot.repository.ImageRepository;
import agh.project.oot.repository.ThumbnailRepository;
import agh.project.oot.thumbnails.ThumbnailConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThumbnailServiceTest {

    @Mock
    private ThumbnailConverter thumbnailConverterMock;

    @Mock
    private ImageRepository imageRepositoryMock;

    @Mock
    private ThumbnailRepository thumbnailRepositoryMock;

    @InjectMocks
    private ThumbnailService thumbnailService;

    /**
     * Processes a list of images, generates thumbnails, and saves them to the database.
     */
    @Test
    void shouldProcessImagesAndSaveThumbnails() {
        byte[] imageData1 = new byte[]{1, 2, 3};
        byte[] imageData2 = new byte[]{4, 5, 6};

        Image image1 = new Image(imageData1, 1L);
        Image image2 = new Image(imageData2, 2L);

        Thumbnail thumbnail = new Thumbnail(imageData1, 1L);

        when(thumbnailConverterMock.generateThumbnail(any(Image.class))).thenReturn(Mono.just(thumbnail));
        when(imageRepositoryMock.save(any(Image.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(thumbnailRepositoryMock.save(any(Thumbnail.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(thumbnailService.saveImagesAndSendThumbnails(List.of(image1, image2)))
                .expectNextCount(2)
                .verifyComplete();

        verify(thumbnailConverterMock, times(2)).generateThumbnail(any(Image.class));
        verify(imageRepositoryMock, times(2)).save(any(Image.class));
        verify(thumbnailRepositoryMock, times(2)).save(any(Thumbnail.class));
    }

    /**
     * Tests that an empty result is returned when a database read error occurs while saving images.
     */
//    @Test
//    void shouldReturnEmptyOnDatabaseReadError() {
//        byte[] imageData1 = new byte[]{1, 2, 3};
//        byte[] imageData2 = new byte[]{4, 5, 6};
//
//        Image image1 = new Image(imageData1, 1L);
//        Image image2 = new Image(imageData2, 2L);
//
//        when(imageRepositoryMock.save(any(Image.class)))
//                .thenReturn(Mono.error(new RuntimeException("Error reading from image repository")));
//
//        when(thumbnailConverterMock.generateThumbnail(any(Image.class)))
//                .thenReturn(Mono.just(new Thumbnail(imageData1, 1L)))
//                .thenReturn(Mono.just(new Thumbnail(imageData2, 2L)));
//
//        StepVerifier.create(thumbnailService.saveImagesAndSendThumbnails(List.of(image1, image2)))
//                .expectNextCount(0)
//                .verifyComplete();
//
//        verify(imageRepositoryMock, times(2)).save(any(Image.class));
//        verify(thumbnailRepositoryMock, times(0)).save(any(Thumbnail.class));
//    }

    /**
     * Retrieves all thumbnails from the database.
     */
    @Test
    void shouldRetrieveAllThumbnails() {
        Thumbnail thumbnail1 = new Thumbnail(new byte[]{1, 2, 3}, 1L);
        Thumbnail thumbnail2 = new Thumbnail(new byte[]{4, 5, 6}, 2L);

        when(thumbnailRepositoryMock.findAll()).thenReturn(Flux.just(thumbnail1, thumbnail2));

        StepVerifier.create(thumbnailService.getAllThumbnails())
                .expectNext(thumbnail1, thumbnail2)
                .verifyComplete();

        verify(thumbnailRepositoryMock).findAll();
    }

    /**
     * Verifies that the method returns an empty Flux when no thumbnails are present in the database.
     */
    @Test
    void shouldReturnEmptyWhenNoThumbnails() {
        when(thumbnailRepositoryMock.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(thumbnailService.getAllThumbnails())
                .expectNextCount(0)
                .verifyComplete();

        verify(thumbnailRepositoryMock).findAll();
    }

    /**
     * Retrieves an image using its associated thumbnail ID.
     */
    @Test
    void shouldRetrieveImageByThumbnailId() {
        Thumbnail thumbnail = new Thumbnail(new byte[]{1, 2, 3}, 1L);
        byte[] imageData = new byte[]{1, 2, 3};
        Image image = new Image(imageData, 1L);

        when(thumbnailRepositoryMock.findById(1L)).thenReturn(Mono.just(thumbnail));
        when(imageRepositoryMock.findById(1L)).thenReturn(Mono.just(image));

        StepVerifier.create(thumbnailService.getImageByThumbnailId(1L))
                .expectNext(image)
                .verifyComplete();

        verify(thumbnailRepositoryMock).findById(1L);
        verify(imageRepositoryMock).findById(1L);
    }

    /**
     * Throws an exception when the thumbnail ID does not exist.
     */
    @Test
    void shouldThrowExceptionWhenThumbnailIdNotFound() {
        when(thumbnailRepositoryMock.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(thumbnailService.getImageByThumbnailId(1L))
                .expectError(NoSuchElementException.class)
                .verify();

        verify(thumbnailRepositoryMock).findById(1L);
        verifyNoInteractions(imageRepositoryMock);
    }
}
