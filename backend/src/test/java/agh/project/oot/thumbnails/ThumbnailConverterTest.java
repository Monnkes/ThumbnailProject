package agh.project.oot.thumbnails;

import agh.project.oot.model.Image;
import agh.project.oot.model.Thumbnail;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ThumbnailConverterTest {

    /**
     * Tests if ThumbnailConverter generates a valid thumbnail with correct dimensions.
     */
    @Test
    void shouldGenerateThumbnail() throws IOException {
        // Given
        int width = 150;
        int height = 150;
        ThumbnailConverter thumbnailConverter = new ThumbnailConverter(width, height);

        byte[] inputData = createFakeImageData();
        Image inputImage = new Image(inputData);

        // When
        Mono<Thumbnail> thumbnailMono = thumbnailConverter.generateThumbnail(inputImage);
        Thumbnail result = thumbnailMono.block();

        // Then
        assertNotNull(result, "Generated thumbnail should not be null");
        assertNotNull(result.getData(), "Thumbnail data should not be null");
        assertIsValidThumbnail(result.getData(), width, height);
    }

    private void assertIsValidThumbnail(byte[] thumbnailData, int expectedWidth, int expectedHeight) throws IOException {
        ByteArrayInputStream thumbnailStream = new ByteArrayInputStream(thumbnailData);
        BufferedImage thumbnailImage = ImageIO.read(thumbnailStream);

        assertNotNull(thumbnailImage, "Thumbnail image should not be null");
        int actualWidth = thumbnailImage.getWidth();
        int actualHeight = thumbnailImage.getHeight();
        assertEquals(expectedWidth, actualWidth, "Thumbnail width should match the expected width");
        assertEquals(expectedHeight, actualHeight, "Thumbnail height should match the expected height");
    }

    private byte[] createFakeImageData() throws IOException {
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", outputStream);
        return outputStream.toByteArray();
    }
}
