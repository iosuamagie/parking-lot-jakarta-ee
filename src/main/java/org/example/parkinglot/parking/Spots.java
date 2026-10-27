package org.example.parkinglot.parking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The 24 spots of the lot: A01–A12 on the top row and B01–B12 on the bottom row. */
public final class Spots {
    public static final int CAPACITY = 24;
    public static final List<String> ALL = createAll();
    /** The spots for people with disabilities, next to the entrance. */
    public static final Set<String> ACCESSIBLE = Set.of("A01", "A02", "A03");

    private Spots() {
    }

    private static List<String> createAll() {
        List<String> spots = new ArrayList<>();
        for (String row : List.of("A", "B")) {
            for (int number = 1; number <= CAPACITY / 2; number++) {
                spots.add(String.format(Locale.ROOT, "%s%02d", row, number));
            }
        }
        return List.copyOf(spots);
    }

    public static boolean isValid(String spot) {
        return spot != null && ALL.contains(spot);
    }
}
