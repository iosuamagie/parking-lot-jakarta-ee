package org.example.parkinglot.parking;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * The days a billing page or report covers, both ends included. Without dates it is the last 7 days.
 */
public record BillingPeriod(LocalDate from, LocalDate to) {
    public static final int DEFAULT_DAYS = 7;
    public static final int MAX_DAYS = 366;
    /** Years the lot can have data for; outside them the date math and the database fail. */
    private static final int FIRST_YEAR = 2000;
    private static final int LAST_YEAR = 2100;

    /** @throws IllegalArgumentException with a message for people, when the dates are not a usable period */
    public static BillingPeriod parse(String from, String to, LocalDate today) {
        boolean noFrom = from == null || from.isBlank();
        boolean noTo = to == null || to.isBlank();
        if (noFrom && noTo) {
            return new BillingPeriod(today.minusDays(DEFAULT_DAYS - 1), today);
        }
        if (noFrom || noTo) {
            throw new IllegalArgumentException("Choose both a start date and an end date.");
        }
        LocalDate start = date(from);
        LocalDate end = date(to);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("The start date is after the end date.");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new IllegalArgumentException("A period can be at most " + MAX_DAYS + " days long.");
        }
        return new BillingPeriod(start, end);
    }

    private static LocalDate date(String value) {
        LocalDate date;
        try {
            date = LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Write the date as YYYY-MM-DD, for example 2026-10-27.", ex);
        }
        if (date.getYear() < FIRST_YEAR || date.getYear() > LAST_YEAR) {
            throw new IllegalArgumentException("Choose a date between the years " + FIRST_YEAR + " and " + LAST_YEAR + ".");
        }
        return date;
    }

    public long days() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
