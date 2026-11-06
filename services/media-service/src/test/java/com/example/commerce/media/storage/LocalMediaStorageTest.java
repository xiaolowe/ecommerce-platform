package com.example.commerce.media.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMediaStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesLoadsAndDeletesPngImage() throws Exception {
        LocalMediaStorage storage = new LocalMediaStorage(tempDir.toString(), "/media");
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.png", "image/png", pngBytes());

        LocalMediaStorage.StoredImage stored = storage.store(file);

        assertThat(stored.width()).isEqualTo(12);
        assertThat(stored.height()).isEqualTo(8);
        assertThat(stored.url()).startsWith("/media/images/");
        assertThat(storage.load(stored.objectKey()).exists()).isTrue();

        storage.delete(stored.objectKey());
        assertThat(tempDir.resolve(stored.objectKey())).doesNotExist();
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
