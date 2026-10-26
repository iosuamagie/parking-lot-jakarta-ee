package org.example.parkinglot.common;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageTypesTest {

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    @Test
    void detectsPng() {
        assertEquals("image/png", ImageTypes.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)));
    }

    @Test
    void detectsJpeg() {
        assertEquals("image/jpeg", ImageTypes.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0)));
    }

    @Test
    void detectsGif() {
        assertEquals("image/gif", ImageTypes.detect("GIF89a".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void detectsWebp() {
        assertEquals("image/webp", ImageTypes.detect("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void rejectsHtmlPretendingToBeAnImage() {
        assertNull(ImageTypes.detect("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsSvg() {
        // SVG can carry scripts, so it is not on the allowed list
        assertNull(ImageTypes.detect("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsEmptyAndTooShortContent() {
        assertNull(ImageTypes.detect(null));
        assertNull(ImageTypes.detect(new byte[0]));
        assertNull(ImageTypes.detect(bytes(0x89, 'P', 'N')));
        assertNull(ImageTypes.detect("RIFF".getBytes(StandardCharsets.US_ASCII)));
    }
}
