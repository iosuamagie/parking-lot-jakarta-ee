package org.example.parkinglot.parking;

import java.time.Duration;
import java.util.Locale;

/**
 * The parking tariff: the first 15 minutes are free, then 6 RON per hour, charged for every started minute.
 * Amounts are whole bani (1 RON = 100 bani), so there are no rounding errors.
 */
public final class Tariff {
    public static final int FREE_MINUTES = 15;
    public static final int BANI_PER_MINUTE = 10;

    private Tariff() {
    }

    public static int amountBani(Duration stay) {
        long paidSeconds = stay.minusMinutes(FREE_MINUTES).toSeconds();
        if (paidSeconds <= 0) {
            return 0;
        }
        long startedMinutes = (paidSeconds + 59) / 60;
        return Math.toIntExact(startedMinutes * BANI_PER_MINUTE);
    }

    /** Writes an amount in Romanian lei (RON), e.g. 1030 → "10.30 RON" and 123450 → "1,234.50 RON". */
    public static String format(int bani) {
        return String.format(Locale.US, "%,d.%02d RON", bani / 100, bani % 100);
    }
}
