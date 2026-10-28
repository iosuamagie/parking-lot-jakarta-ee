package org.example.parkinglot.documents;

import org.example.parkinglot.common.TicketDto;
import org.example.parkinglot.parking.AcceleratedClock;
import org.example.parkinglot.parking.Tariff;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A till-style receipt, 80 mm wide and as tall as its content. */
public final class ReceiptPdf {
    /** 80 mm in PDF points (1 pt = 1/72 inch). */
    static final float WIDTH = 226.77f;
    private static final float MARGIN = 14f;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(AcceleratedClock.ZONE);
    private static final Color MUTED = new Color(0x6b, 0x72, 0x80);

    private ReceiptPdf() {
    }

    public static byte[] render(TicketDto ticket) {
        if (ticket.exitedAt() == null) {
            throw new IllegalArgumentException("The car is still parked, so there is no receipt yet");
        }
        PdfPTable content = content(ticket);
        content.setTotalWidth(WIDTH - 2 * MARGIN);
        content.setLockedWidth(true);
        float height = content.getTotalHeight() + 2 * MARGIN;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(new Rectangle(WIDTH, height), MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter.getInstance(document, out);
        document.addTitle("Receipt " + ticket.ticketNumber());
        document.open();
        document.add(content);
        document.close();
        return out.toByteArray();
    }

    private static PdfPTable content(TicketDto ticket) {
        PdfPTable table = new PdfPTable(new float[]{1f, 1.5f});
        centered(table, "ParkPulse · Receipt", PdfFonts.bold(13), 2);
        centered(table, "Public parking", PdfFonts.regular(8), 10);
        line(table, "Ticket", ticket.ticketNumber());
        line(table, "Plate", ticket.plate());
        line(table, "Spot", ticket.spot());
        line(table, "Entered", DATE_TIME.format(ticket.enteredAt()));
        line(table, "Left", DATE_TIME.format(ticket.exitedAt()));
        line(table, "Stay", duration(ticket.enteredAt(), ticket.exitedAt()));

        PdfPCell tariff = cell(new Phrase("Tariff: the first 15 minutes are free, then 6 RON/hour for every started minute.",
                font(PdfFonts.regular(7.5f), MUTED)));
        tariff.setColspan(2);
        tariff.setPaddingTop(6);
        tariff.setPaddingBottom(8);
        tariff.setBorder(Rectangle.BOTTOM);
        tariff.setBorderColor(new Color(0xd1, 0xd5, 0xdb));
        table.addCell(tariff);

        PdfPCell totalLabel = cell(new Phrase("TOTAL", PdfFonts.bold(12)));
        totalLabel.setPaddingTop(8);
        table.addCell(totalLabel);
        PdfPCell total = cell(new Phrase(Tariff.format(ticket.amountBani() == null ? 0 : ticket.amountBani()),
                PdfFonts.bold(12)));
        total.setHorizontalAlignment(Element.ALIGN_RIGHT);
        total.setPaddingTop(8);
        table.addCell(total);

        centered(table, "Thank you!", PdfFonts.bold(10), 12);
        centered(table, "Demo document, not a fiscal receipt.", font(PdfFonts.regular(7), MUTED), 4);
        return table;
    }

    private static String duration(Instant from, Instant to) {
        long minutes = Duration.between(from, to).toMinutes();
        return minutes < 60 ? minutes + " min" : (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private static Font font(Font font, Color color) {
        font.setColor(color);
        return font;
    }

    private static PdfPCell cell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2);
        return cell;
    }

    private static void centered(PdfPTable table, String text, Font font, float paddingTop) {
        PdfPCell cell = cell(new Phrase(text, font));
        cell.setColspan(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(paddingTop);
        table.addCell(cell);
    }

    private static void line(PdfPTable table, String label, String value) {
        table.addCell(cell(new Phrase(label, font(PdfFonts.regular(9), MUTED))));
        PdfPCell right = cell(new Phrase(value, PdfFonts.regular(9)));
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(right);
    }
}
