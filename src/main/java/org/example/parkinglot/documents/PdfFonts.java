package org.example.parkinglot.documents;

import org.openpdf.text.Font;
import org.openpdf.text.pdf.BaseFont;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Liberation Sans from openpdf-fonts-extra, embedded in the PDF with Unicode (Identity-H) encoding.
 * The standard PDF fonts have no ă, ș or ț, so Romanian text needs a real font.
 */
public final class PdfFonts {
    private static final BaseFont REGULAR = load("LiberationSans-Regular.ttf");
    private static final BaseFont BOLD = load("LiberationSans-Bold.ttf");

    private PdfFonts() {
    }

    public static Font regular(float size) {
        return new Font(REGULAR, size);
    }

    public static Font bold(float size) {
        return new Font(BOLD, size);
    }

    private static BaseFont load(String fileName) {
        try (InputStream in = PdfFonts.class.getResourceAsStream("/liberation/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException("The font " + fileName + " is missing from openpdf-fonts-extra");
            }
            return BaseFont.createFont(fileName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
