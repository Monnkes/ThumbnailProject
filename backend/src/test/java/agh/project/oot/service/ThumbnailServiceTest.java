//package agh.project.oot.service;
//
//import agh.project.oot.model.Image;
//import agh.project.oot.model.Thumbnail;
//import agh.project.oot.model.ThumbnailType;
//import agh.project.oot.repository.ThumbnailRepository;
//import agh.project.oot.thumbnails.ThumbnailConverter;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.util.NoSuchElementException;
//
//import static agh.project.oot.model.ThumbnailType.SMALL;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ThumbnailServiceTest {
//
//    @Mock
//    private ThumbnailConverter thumbnailConverterMock;
//
//    @Mock
//    private ImageService imageService;
//
//    @Mock
//    private ThumbnailRepository thumbnailRepositoryMock;
//
//    @InjectMocks
//    private ThumbnailService thumbnailService;
//
//    /**
//     * Processes a list of images, generates thumbnails, and saves them to the database.
//     */
//    @Test
//    void shouldProcessImagesAndSaveThumbnails() {
//        // Given
//        byte[] imageData1 = new byte[]{1, 2, 3};
//        byte[] imageData2 = new byte[]{4, 5, 6};
//
//        Image image1 = new Image(imageData1, 1L);
//        Image image2 = new Image(imageData2, 2L);
//
//        Thumbnail thumbnail = new Thumbnail(imageData1, 1L, SMALL);
//
//        // When
//        when(thumbnailConverterMock.generateThumbnail(any(Image.class), eq(150), eq(150), any(ThumbnailType.class)))
//                .thenReturn(Mono.just(thumbnail));
//        when(imageService.saveAndNotifyThumbnail(any(Image.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
//        when(thumbnailRepositoryMock.save(any(Thumbnail.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
//
//        // Then
//        StepVerifier.create(imageService.saveAndNotifyThumbnail(image1))
//                .expectNextCount(1)
//                .verifyComplete();
//
//        StepVerifier.create(imageService.saveAndNotifyThumbnail(image2))
//                .expectNextCount(1)
//                .verifyComplete();
//
//        verify(thumbnailConverterMock, times(2))
//                .generateThumbnail(any(Image.class), eq(150), eq(150), any(ThumbnailType.class));
//        verify(imageService, times(2)).saveAndNotifyThumbnail(any(Image.class));
//        verify(thumbnailRepositoryMock, times(2)).save(any(Thumbnail.class));
//    }
//
//    /**
//     * Tests that an error is thrown when a database read error occurs while saving images.
//     */
//    @Test
//    void shouldThrowErrorOnDatabaseReadError() {
//        // Given
//        byte[] imageData1 = new byte[]{1, 2, 3};
//        byte[] imageData2 = new byte[]{4, 5, 6};
//
//        Image image1 = new Image(imageData1, 1L);
//        Image image2 = new Image(imageData2, 2L);
//
//        Thumbnail thumbnail2 = new Thumbnail(imageData2, 2L, SMALL);
//
//        // When
//        when(imageService.saveAndNotifyThumbnail(image1))
//                .thenReturn(Mono.error(new IllegalArgumentException("Error saving image to repository")));
//
//        when(imageService.saveAndNotifyThumbnail(image2)).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
//        when(thumbnailRepositoryMock.save(thumbnail2)).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
//
//        when(thumbnailConverterMock.generateThumbnail(any(Image.class), eq(150), eq(150), any(ThumbnailType.class)))
//                .thenReturn(Mono.just(new Thumbnail(imageData1, 1L, SMALL)))
//                .thenReturn(Mono.just(new Thumbnail(imageData2, 2L, SMALL)));
//
//        // Then
//        StepVerifier.create(imageService.saveAndNotifyThumbnail(image1))
//                .expectError(IllegalArgumentException.class)
//                .verify();
//
//        StepVerifier.create(imageService.saveAndNotifyThumbnail(image2))
//                .expectNextCount(1)
//                .verifyComplete();
//
//        verify(imageService, times(2)).saveAndNotifyThumbnail(any(Image.class));
//        verify(thumbnailRepositoryMock, times(1)).save(thumbnail2);
//    }
//
//    /**
//     * Retrieves all thumbnails from the database.
//     */
//    @Test
//    void shouldRetrieveAllThumbnails() {
//        // Given
//        Thumbnail thumbnail1 = new Thumbnail(new byte[]{1, 2, 3}, 1L, SMALL);
//        Thumbnail thumbnail2 = new Thumbnail(new byte[]{4, 5, 6}, 2L, SMALL);
//
//        // When
//        when(thumbnailRepositoryMock.findAll()).thenReturn(Flux.just(thumbnail1, thumbnail2));
//
//        // Then
//        StepVerifier.create(thumbnailService.getAllThumbnailsByType(eq(SMALL)))
//                .expectNext(thumbnail1, thumbnail2)
//                .verifyComplete();
//
//        verify(thumbnailRepositoryMock).findAll();
//    }
//
//    /**
//     * Verifies that the method returns an empty Flux when no thumbnails are present in the database.
//     */
//    @Test
//    void shouldReturnEmptyWhenNoThumbnails() {
//        // When
//        when(thumbnailRepositoryMock.findAll()).thenReturn(Flux.empty());
//
//        // Then
//        StepVerifier.create(thumbnailService.getAllThumbnailsByType(eq(SMALL)))
//                .expectNextCount(0)
//                .verifyComplete();
//
//        verify(thumbnailRepositoryMock).findAll();
//    }
//
//    /**
//     * Retrieves an image using its associated thumbnail ID.
//     */
//    @Test
//    void shouldRetrieveImageByThumbnailId() {
//        // Given
//        Thumbnail thumbnail = new Thumbnail(new byte[]{1, 2, 3}, 1L, SMALL);
//        byte[] imageData = new byte[]{1, 2, 3};
//        Image image = new Image(imageData, 1L);
//
//        // When
//        when(thumbnailRepositoryMock.findById(1L)).thenReturn(Mono.just(thumbnail));
//        when(imageService.findById(1L)).thenReturn(Mono.just(image));
//
//        // Then
//        StepVerifier.create(thumbnailService.getImageByThumbnailId(1L))
//                .expectNext(image)
//                .verifyComplete();
//
//        verify(thumbnailRepositoryMock).findById(1L);
//        verify(imageService).findById(1L);
//    }
//
//    /**
//     * Throws an exception when the thumbnail ID does not exist.
//     */
//    @Test
//    void shouldThrowExceptionWhenThumbnailIdNotFound() {
//        // When
//        when(thumbnailRepositoryMock.findById(1L)).thenReturn(Mono.empty());
//
//        // Then
//        StepVerifier.create(thumbnailService.getImageByThumbnailId(1L))
//                .expectError(NoSuchElementException.class)
//                .verify();
//
//        verify(thumbnailRepositoryMock).findById(1L);
//        verifyNoInteractions(imageService);
//    }
//}
