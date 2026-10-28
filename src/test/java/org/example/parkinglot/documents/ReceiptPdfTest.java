package org.example.parkinglot.documents;

import org.example.parkinglot.common.TicketDto;
import org.example.parkinglot.parking.CarModel;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptPdfTest {

    static String textOf(byte[] pdf) throws IOException {
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        }
    }

    private static TicketDto finishedTicket() {
        return new TicketDto("AAAAAAAAAAAAAAAAAAAAAA", "PP-00042", "SB-47-XYZ", "A08", CarModel.SEDAN, "#3498db",
                false, Instant.parse("2026-10-27T10:00:00Z"), Instant.parse("2026-10-27T11:58:00Z"), 1030,
                Instant.parse("2026-10-27T12:00:00Z"), 60);
    }

    @Test
    void theReceiptShowsTheTicketWithRomanianLetters() throws IOException {
        String text = textOf(ReceiptPdf.render(finishedTicket()));
        assertTrue(text.contains("PP-00042"), text);
        assertTrue(text.contains("SB-47-XYZ"), text);
        assertTrue(text.contains("10.30 RON"), text);
        assertTrue(text.contains("Receipt"), text);
        assertTrue(text.contains("13:58"), text); // Romanian time (UTC+2)
    }

    @Test
    void theReceiptIsEightyMillimetresWideOnOnePage() throws IOException {
        try (PdfReader reader = new PdfReader(ReceiptPdf.render(finishedTicket()))) {
            assertEquals(1, reader.getNumberOfPages());
            assertEquals(226.77f, reader.getPageSize(1).getWidth(), 0.1f);
        }
    }
}
