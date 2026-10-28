package org.example.parkinglot.parking;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Makes up Romanian-looking plates (SB-07-KTR, B-123-ABC) for the simulated cars. */
public final class PlateGenerator {
    private static final List<String> COUNTIES = List.of("SB", "B", "CJ", "BV", "AG", "TM", "IS", "MS", "HD", "VL", "AB");
    /** Romanian plates do not use I, O and Q in the last group, because they look like digits. */
    private static final String LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ";

    private PlateGenerator() {
    }

    /** @return a plate that is not in {@code taken}; the caller adds it there when the car enters */
    public static String next(RandomGenerator random, Set<String> taken) {
        while (true) {
            String plate = random(random);
            if (!taken.contains(plate)) {
                return plate;
            }
        }
    }

    private static String random(RandomGenerator random) {
        String county = COUNTIES.get(random.nextInt(COUNTIES.size()));
        int number = county.equals("B") ? random.nextInt(100, 1000) : random.nextInt(1, 100);
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            letters.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        return String.format(Locale.ROOT, "%s-%02d-%s", county, number, letters);
    }
}
