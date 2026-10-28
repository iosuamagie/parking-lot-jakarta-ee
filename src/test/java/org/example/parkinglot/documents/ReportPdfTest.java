package org.example.parkinglot.documents;

import org.example.parkinglot.common.BillingSummaryDto;
import org.example.parkinglot.common.DayTotalDto;
import org.example.parkinglot.common.PaidTicketDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportPdfTest {

    @Test
    void theReportListsTotalsAndDays() throws IOException {
        LocalDate day1 = LocalDate.of(2026, 10, 26), day2 = LocalDate.of(2026, 10, 27);
        BillingSummaryDto summary = new BillingSummaryDto(day1, day2, 1430, 1000, 2, 715, 1,
                List.of(new DayTotalDto(day1, 400, 1), new DayTotalDto(day2, 1030, 2)));
        List<PaidTicketDto> tickets = List.of(
                new PaidTicketDto("a", "PP-00001", "SB-01-ABC", "A04", Instant.parse("2026-10-26T08:00:00Z"),
                        Instant.parse("2026-10-26T08:55:00Z"), 400),
                new PaidTicketDto("b", "PP-00002", "B-123-XYZ", "B05", Instant.parse("2026-10-27T10:00:00Z"),
                        Instant.parse("2026-10-27T11:58:00Z"), 1030),
                new PaidTicketDto("c", "PP-00003", "CJ-10-EJB", "B06", Instant.parse("2026-10-27T12:00:00Z"),
                        Instant.parse("2026-10-27T12:10:00Z"), 0));

        String text = ReceiptPdfTest.textOf(ReportPdf.render(summary, tickets));

        assertTrue(text.contains("Billing report"), text);
        assertTrue(text.contains("Total"), text);
        assertTrue(text.contains("14.30 RON"), text);
        assertTrue(text.contains("27 Oct 2026"), text);
        for (String number : List.of("PP-00001", "PP-00002", "PP-00003")) {
            assertTrue(text.contains(number), number);
        }
        assertTrue(text.contains("free"), text);
    }
}
