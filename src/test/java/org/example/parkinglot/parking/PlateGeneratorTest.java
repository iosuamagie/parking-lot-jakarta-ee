package org.example.parkinglot.parking;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateGeneratorTest {
    private final RandomGenerator rnd = new SplittableRandom(7);

    @Test
    void platesLookRomanianAndAreNotTaken() {
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            String plate = PlateGenerator.next(rnd, taken);
            assertTrue(plate.matches("[A-Z]{1,2}-\\d{2,3}-[A-HJ-NPR-Z]{3}"), plate);
            assertTrue(plate.startsWith("B-") || plate.matches("[A-Z]{2}-\\d{2}-.*"), plate);
            assertTrue(taken.add(plate), plate);
        }
    }
}
