package org.example.parkinglot.parking;

import java.util.List;
import java.util.random.RandomGenerator;

/** Picks how a simulated car looks: its model and its body color. */
public final class CarLooks {
    public static final String TAXI_YELLOW = "#f5c518";
    public static final List<String> PALETTE = List.of(
            "#e53935", "#1e88e5", "#43a047", "#fb8c00", "#8e24aa",
            "#00acc1", "#f4f4f5", "#37474f", "#9e9e9e", "#6d4c41");

    private CarLooks() {
    }

    /** Sedans are the most common, vans the rarest. */
    public static CarModel randomModel(RandomGenerator random) {
        int roll = random.nextInt(100);
        if (roll < 35) {
            return CarModel.SEDAN;
        }
        if (roll < 55) {
            return CarModel.MINI;
        }
        if (roll < 75) {
            return CarModel.SUV;
        }
        if (roll < 88) {
            return CarModel.TAXI;
        }
        return CarModel.VAN;
    }

    public static String randomColor(RandomGenerator random, CarModel model) {
        if (model == CarModel.TAXI) {
            return TAXI_YELLOW;
        }
        return PALETTE.get(random.nextInt(PALETTE.size()));
    }
}
