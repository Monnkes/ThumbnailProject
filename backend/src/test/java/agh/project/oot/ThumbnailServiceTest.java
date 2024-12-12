package agh.project.oot;

import static org.mockito.Mockito.*;

import agh.project.oot.database.ImageRepository;
import agh.project.oot.database.Thumbnail;
import agh.project.oot.database.Image;
import agh.project.oot.database.ThumbnailRepository;
import agh.project.oot.thumbnails.ThumbnailConverter;
import agh.project.oot.thumbnails.ThumbnailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

class ThumbnailServiceTest {

    @Mock
    private ThumbnailConverter thumbnailConverter;

    @Mock
    private ThumbnailRepository thumbnailRepository;

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private ThumbnailService thumbnailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveImages() {
        byte[] image1 = {1, 2, 3};
        byte[] image2 = {4, 5, 6};
        List<byte[]> images = List.of(image1, image2);

//        when(thumbnailConverter.generateThumbnails(any(Flux.class), eq(100), eq(100)))
//                .thenReturn(Flux.just(new byte[]{10, 20, 30}, new byte[]{40, 50, 60}));
//
//        when(thumbnailRepository.save(any(Thumbnail.class)))
//                .thenReturn(Mono.just(new Thumbnail()));
//        when(imageRepository.save(any(Image.class)))
//                .thenReturn(Mono.just(new Image()));


//        thumbnailService.saveImagesAndSendThumbnails(images, 100, 100);
//
//        verify(thumbnailConverter).generateThumbnails(any(Flux.class), eq(100), eq(100));
//        verify(thumbnailRepository, times(2)).save(any(Thumbnail.class));
//        verify(imageRepository, times(2)).save(any(Image.class));
    }
}
