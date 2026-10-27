package org.example.parkinglot.parking;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarLooksTest {
    private final RandomGenerator rnd = new SplittableRandom(7);

    @Test
    void taxisAreYellowAndOtherColorsComeFromThePalette() {
        assertEquals(10, CarLooks.PALETTE.size());
        assertEquals("#f5c518", CarLooks.randomColor(rnd, CarModel.TAXI));
        for (int i = 0; i < 50; i++) {
            assertTrue(CarLooks.PALETTE.contains(CarLooks.randomColor(rnd, CarModel.SEDAN)));
        }
    }
}
