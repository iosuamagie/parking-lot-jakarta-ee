package org.example.parkinglot.parking;

import org.example.parkinglot.common.ParkedCarDto;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/** The decisions of the traffic simulator, kept apart from the EJB so they can be tested. */
public final class SimulatorPolicy {
    /** A parked car may leave after this long on the parking clock. */
    static final Duration MIN_STAY = Duration.ofMinutes(20);

    public enum Action {
        ENTER, EXIT, NOTHING
    }

    private SimulatorPolicy() {
    }

    /** Keeps the lot between about half and seventy percent full. */
    public static Action decide(int occupied, int capacity, RandomGenerator random) {
        if (occupied <= 0) {
            return Action.ENTER;
        }
        if (occupied >= capacity) {
            return Action.EXIT;
        }
        double fill = (double) occupied / capacity;
        double enterChance = fill < 0.5 ? 0.8 : fill > 0.7 ? 0.2 : 0.5;
        return random.nextDouble() < enterChance ? Action.ENTER : Action.EXIT;
    }

    /**
     * Cars with a disability permit take an accessible spot when one is free; other cars never take one.
     *
     * @return empty when the car has nowhere to park
     */
    public static Optional<String> chooseSpot(Set<String> freeSpots, boolean permit, RandomGenerator random) {
        if (permit) {
            Optional<String> accessible = pick(freeSpots.stream().filter(Spots.ACCESSIBLE::contains).toList(), random);
            if (accessible.isPresent()) {
                return accessible;
            }
        }
        return pick(freeSpots.stream().filter(spot -> !Spots.ACCESSIBLE.contains(spot)).toList(), random);
    }

    /** About one car in ten has a disability permit. */
    public static boolean drawPermit(RandomGenerator random) {
        return random.nextInt(10) == 0;
    }

    /** Real time until the next car moves. */
    public static Duration nextDelay(RandomGenerator random) {
        return Duration.ofMillis(random.nextLong(3000, 8001));
    }

    /**
     * Picks a random car among those that stayed at least {@link #MIN_STAY}, or the oldest one if none did.
     *
     * @return the spot of the car that leaves
     */
    public static String pickLeaving(List<ParkedCarDto> parked, Instant now, RandomGenerator random) {
        if (parked.isEmpty()) {
            throw new IllegalArgumentException("No car is parked");
        }
        List<ParkedCarDto> stayedLongEnough = parked.stream()
                .filter(car -> !car.enteredAt().plus(MIN_STAY).isAfter(now))
                .toList();
        if (stayedLongEnough.isEmpty()) {
            return parked.stream().min(Comparator.comparing(ParkedCarDto::enteredAt)).orElseThrow().spot();
        }
        return stayedLongEnough.get(random.nextInt(stayedLongEnough.size())).spot();
    }

    private static Optional<String> pick(List<String> spots, RandomGenerator random) {
        if (spots.isEmpty()) {
            return Optional.empty();
        }
        List<String> sorted = spots.stream().sorted().toList();
        return Optional.of(sorted.get(random.nextInt(sorted.size())));
    }
}
