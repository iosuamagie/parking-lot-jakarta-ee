package org.example.parkinglot.parking;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;

/**
 * The parking lot's own clock. It runs {@code speed} times faster than real time, so that a demo
 * visitor sees stays of hours (and real prices) within a few minutes.
 * <p>
 * When the computer sleeps, real time jumps forward while nothing in the lot moves. A gap longer than
 * {@link #MAX_GAP} between two readings counts only as {@code MAX_GAP}, so parked cars do not suddenly
 * owe for days. The application reads the clock every few seconds, so normal gaps are short.
 */
public final class AcceleratedClock {
    public static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");
    static final Duration MAX_GAP = Duration.ofMinutes(2);

    private final Instant parkingStart;
    private final int speed;
    private final Supplier<Instant> realNow;
    private Instant realStart;
    private Instant lastReading;

    public AcceleratedClock(Instant parkingStart, Instant realStart, int speed, Supplier<Instant> realNow) {
        this.parkingStart = parkingStart;
        this.realStart = realStart;
        this.speed = speed;
        this.realNow = realNow;
        this.lastReading = realStart;
    }

    public synchronized Instant now() {
        Instant real = realNow.get();
        Duration gap = Duration.between(lastReading, real);
        if (gap.compareTo(MAX_GAP) > 0) {
            // The time beyond MAX_GAP did not happen for the parking lot
            realStart = realStart.plus(gap.minus(MAX_GAP));
        }
        if (real.isAfter(lastReading)) {
            lastReading = real;
        }
        Duration realElapsed = Duration.between(realStart, real);
        return parkingStart.plus(realElapsed.multipliedBy(speed));
    }

    public int speed() {
        return speed;
    }
}
