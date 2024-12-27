package agh.project.oot.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.IOException;

@Slf4j
public class ImageReader {
    public static byte[] loadImageAsBytes(String imagePath) {
        InputStream inputStream = ImageReader.class.getClassLoader().getResourceAsStream(imagePath);

        if (inputStream == null) {
            log.error("Obraz nie został znaleziony w zasobach: {}", imagePath);
        }

        try {
            assert inputStream != null;
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
