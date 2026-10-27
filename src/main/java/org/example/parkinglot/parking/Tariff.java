package org.example.parkinglot.parking;

import java.time.Duration;
import java.util.Locale;

/**
 * The parking tariff: the first 15 minutes are free, then 6 lei per hour, charged for every started minute.
 * Amounts are whole bani (1 leu = 100 bani), so there are no rounding errors.
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

    /** Writes an amount the Romanian way, e.g. 1030 → "10,30 lei". */
    public static String format(int bani) {
        return String.format(Locale.ROOT, "%d,%02d lei", bani / 100, bani % 100);
    }
}
