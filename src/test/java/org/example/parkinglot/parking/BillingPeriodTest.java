package org.example.parkinglot.parking;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillingPeriodTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    @Test
    void theDefaultIsTheLastSevenDays() {
        BillingPeriod period = BillingPeriod.parse(null, null, TODAY);
        assertEquals(LocalDate.of(2026, 10, 21), period.from());
        assertEquals(TODAY, period.to());
        assertEquals(BillingPeriod.parse("", " ", TODAY), period);
    }

    @Test
    void aChosenPeriodIsKept() {
        BillingPeriod period = BillingPeriod.parse("2026-10-01", "2026-10-01", TODAY);
        assertEquals(LocalDate.of(2026, 10, 1), period.from());
        assertEquals(LocalDate.of(2026, 10, 1), period.to());
    }

    @Test
    void badDatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("ieri", "2026-10-27", TODAY));
        assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("2026-10-27", "2026-10-01", TODAY));
        assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("2024-01-01", "2026-10-27", TODAY));
        assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("2026-10-01", null, TODAY));
        // Signed years parse as dates but break the date math and the database
        assertThrows(IllegalArgumentException.class,
                () -> BillingPeriod.parse("+999999999-12-31", "+999999999-12-31", TODAY));
        assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("+100000-01-01", "+100000-01-01", TODAY));
        assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("1999-12-31", "1999-12-31", TODAY));
    }
}
