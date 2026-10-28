package org.example.parkinglot.documents;

import org.example.parkinglot.common.BillingSummaryDto;
import org.example.parkinglot.common.DayTotalDto;
import org.example.parkinglot.common.PaidTicketDto;
import org.example.parkinglot.parking.AcceleratedClock;
import org.example.parkinglot.parking.Tariff;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** The billing report for a period: totals, a table per day and every paid ticket, on A4 pages. */
public final class ReportPdf {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(AcceleratedClock.ZONE);
    private static final Color HEADER = new Color(0xee, 0xf2, 0xff);
    private static final Color MUTED = new Color(0x6b, 0x72, 0x80);
    private static final Color LINE = new Color(0xe5, 0xe7, 0xeb);

    private ReportPdf() {
    }

    public static byte[] render(BillingSummaryDto summary, List<PaidTicketDto> tickets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, out);
        document.addTitle("Billing report");
        document.open();

        document.add(new Paragraph("ParkPulse · Billing report", PdfFonts.bold(18)));
        Paragraph period = new Paragraph("Period " + DAY.format(summary.from()) + " – " + DAY.format(summary.to()),
                colored(PdfFonts.regular(10), MUTED));
        period.setSpacingAfter(14);
        document.add(period);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(60);
        totals.setHorizontalAlignment(Element.ALIGN_LEFT);
        totals.setSpacingAfter(16);
        pair(totals, "Total", Tariff.format(summary.totalBani()), true);
        pair(totals, "Previous period", Tariff.format(summary.previousPeriodTotalBani()), false);
        pair(totals, "Paid tickets", String.valueOf(summary.paidTickets()), false);
        pair(totals, "Average per ticket", Tariff.format(summary.averageBani()), false);
        pair(totals, "Gratuite (sub 15 minute)", String.valueOf(summary.freeTickets()), false);
        document.add(totals);

        document.add(heading("Per day"));
        PdfPTable days = table(new float[]{2, 1, 1.5f}, "Day", "Tickets", "Total");
        for (DayTotalDto day : summary.days()) {
            cells(days, DAY.format(day.day()), String.valueOf(day.tickets()), Tariff.format(day.totalBani()));
        }
        days.setSpacingAfter(16);
        document.add(days);

        document.add(heading("Paid tickets"));
        PdfPTable list = table(new float[]{1.2f, 1.4f, 0.7f, 1.6f, 1.6f, 1.2f},
                "Ticket", "Plate", "Spot", "Entered", "Left", "Amount");
        for (PaidTicketDto ticket : tickets) {
            cells(list, ticket.ticketNumber(), ticket.plate(), ticket.spot(), DATE_TIME.format(ticket.enteredAt()),
                    DATE_TIME.format(ticket.exitedAt()),
                    ticket.amountBani() == 0 ? "free" : Tariff.format(ticket.amountBani()));
        }
        document.add(list);

        document.close();
        return out.toByteArray();
    }

    private static Font colored(Font font, Color color) {
        font.setColor(color);
        return font;
    }

    private static Paragraph heading(String text) {
        Paragraph heading = new Paragraph(text, PdfFonts.bold(12));
        heading.setSpacingAfter(6);
        return heading;
    }

    private static void pair(PdfPTable table, String label, String value, boolean strong) {
        PdfPCell left = new PdfPCell(new Phrase(label, colored(PdfFonts.regular(10), MUTED)));
        PdfPCell right = new PdfPCell(new Phrase(value, strong ? PdfFonts.bold(12) : PdfFonts.regular(10)));
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell cell : List.of(left, right)) {
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    /** A table whose first row (the column names) repeats at the top of every page. */
    private static PdfPTable table(float[] widths, String... headers) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, PdfFonts.bold(9)));
            cell.setBackgroundColor(HEADER);
            cell.setBorderColor(LINE);
            cell.setPadding(5);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
        return table;
    }

    private static void cells(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, PdfFonts.regular(9)));
            cell.setBorderColor(LINE);
            cell.setPadding(4);
            table.addCell(cell);
        }
    }
}
