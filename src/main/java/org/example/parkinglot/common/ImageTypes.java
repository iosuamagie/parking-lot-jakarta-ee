package org.example.parkinglot.common;

/**
 * Finds the real type of an uploaded image from its first bytes, instead of trusting what the browser sends.
 */
public final class ImageTypes {

    private ImageTypes() {
    }

    /**
     * @return the MIME type of a PNG, JPEG, GIF or WebP image, or {@code null} for anything else
     */
    public static String detect(byte[] content) {
        if (content == null) {
            return null;
        }
        if (startsWith(content, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (startsWith(content, 0, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(content, 0, 'G', 'I', 'F', '8')) {
            return "image/gif";
        }
        if (startsWith(content, 0, 'R', 'I', 'F', 'F') && startsWith(content, 8, 'W', 'E', 'B', 'P')) {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] content, int offset, int... signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
