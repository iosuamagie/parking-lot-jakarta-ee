package org.example.parkinglot.parking;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;

/**
 * The parking lot's own clock. It runs {@code speed} times faster than real time, so that a demo
 * visitor sees stays of hours (and real prices) within a few minutes.
 */
public final class AcceleratedClock {
    public static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");

    private final Instant parkingStart;
    private final Instant realStart;
    private final int speed;
    private final Supplier<Instant> realNow;

    public AcceleratedClock(Instant parkingStart, Instant realStart, int speed, Supplier<Instant> realNow) {
        this.parkingStart = parkingStart;
        this.realStart = realStart;
        this.speed = speed;
        this.realNow = realNow;
    }

    public Instant now() {
        Duration realElapsed = Duration.between(realStart, realNow.get());
        return parkingStart.plus(realElapsed.multipliedBy(speed));
    }

    public int speed() {
        return speed;
    }
}
