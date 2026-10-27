package org.example.parkinglot.documents;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrSvgTest {

    @Test
    void everyMatrixRowIsAGroupInsideAQuietZone() {
        String svg = QrSvg.svg("http://localhost:8081/bilet/AAAAAAAAAAAAAAAAAAAAAA");
        Matcher matcher = Pattern.compile("viewBox=\"0 0 (\\d+) \\1\"").matcher(svg);
        assertTrue(matcher.find());
        int size = Integer.parseInt(matcher.group(1));
        assertEquals(size - 4, svg.split("class=\"qr-row\"", -1).length - 1);
        assertTrue(svg.contains("style=\"--row:0\""));
    }

    @Test
    void theTextIsNotCopiedIntoTheSvg() {
        // The SVG holds only squares, so nothing from the address can turn into markup
        String svg = QrSvg.svg("http://x/<script>alert(1)</script>");
        assertFalse(svg.contains("<script"));
    }
}
