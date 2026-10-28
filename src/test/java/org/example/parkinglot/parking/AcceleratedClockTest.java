package org.example.parkinglot.parking;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcceleratedClockTest {

    @Test
    void oneRealSecondIsOneParkingMinute() {
        Instant real0 = Instant.parse("2026-10-27T10:00:00Z");
        Instant park0 = Instant.parse("2026-01-01T08:00:00Z");
        AtomicReference<Instant> real = new AtomicReference<>(real0);
        AcceleratedClock clock = new AcceleratedClock(park0, real0, 60, real::get);

        real.set(real0.plusSeconds(90));

        assertEquals(park0.plus(Duration.ofMinutes(90)), clock.now());
    }

    @Test
    void aSleepingComputerDoesNotMakeTheClockJumpDays() {
        Instant real0 = Instant.parse("2026-10-27T10:00:00Z");
        Instant park0 = Instant.parse("2026-01-01T08:00:00Z");
        AtomicReference<Instant> real = new AtomicReference<>(real0);
        AcceleratedClock clock = new AcceleratedClock(park0, real0, 60, real::get);

        real.set(real0.plusSeconds(10));
        assertEquals(park0.plus(Duration.ofMinutes(10)), clock.now());

        // The computer sleeps for 3 hours: only the first 2 real minutes count
        real.set(real0.plusSeconds(10).plus(Duration.ofHours(3)));
        assertEquals(park0.plus(Duration.ofMinutes(10 + 120)), clock.now());

        real.set(real0.plusSeconds(11).plus(Duration.ofHours(3)));
        assertEquals(park0.plus(Duration.ofMinutes(10 + 120 + 1)), clock.now());
    }
}
