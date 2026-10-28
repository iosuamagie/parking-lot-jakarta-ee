package org.example.parkinglot.documents;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Draws a QR code as SVG squares. Each row of the code is its own group with a {@code --row} number,
 * so the page can "print" the code row by row.
 */
public final class QrSvg {
    /** Empty modules around the code, so phones can find its edges. */
    private static final int QUIET_ZONE = 2;

    private QrSvg() {
    }

    public static String svg(String text) {
        ByteMatrix matrix;
        try {
            matrix = Encoder.encode(text, ErrorCorrectionLevel.M,
                    Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name())).getMatrix();
        } catch (WriterException ex) {
            throw new IllegalArgumentException("This text does not fit in a QR code", ex);
        }
        int size = matrix.getWidth() + 2 * QUIET_ZONE;
        StringBuilder svg = new StringBuilder()
                .append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(size).append(' ').append(size)
                .append("\" shape-rendering=\"crispEdges\">")
                .append("<rect width=\"").append(size).append("\" height=\"").append(size).append("\" fill=\"#fff\"/>");
        for (int y = 0; y < matrix.getHeight(); y++) {
            svg.append("<g class=\"qr-row\" style=\"--row:").append(y).append("\">");
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y) == 1) {
                    svg.append("<rect x=\"").append(x + QUIET_ZONE).append("\" y=\"").append(y + QUIET_ZONE)
                            .append("\" width=\"1\" height=\"1\"/>");
                }
            }
            svg.append("</g>");
        }
        return svg.append("</svg>").toString();
    }
}
