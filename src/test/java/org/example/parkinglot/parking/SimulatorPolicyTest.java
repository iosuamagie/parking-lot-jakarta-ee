package org.example.parkinglot.parking;

import org.example.parkinglot.common.ParkedCarDto;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static org.example.parkinglot.parking.SimulatorPolicy.Action.ENTER;
import static org.example.parkinglot.parking.SimulatorPolicy.Action.EXIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatorPolicyTest {
    private final RandomGenerator rnd = new SplittableRandom(7);

    private long count(SimulatorPolicy.Action action, int occupied) {
        long count = 0;
        for (int i = 0; i < 1000; i++) {
            if (SimulatorPolicy.decide(occupied, 24, rnd) == action) {
                count++;
            }
        }
        return count;
    }

    @Test
    void anEmptyLotAlwaysLetsACarIn() {
        assertEquals(1000, count(ENTER, 0));
    }

    @Test
    void aFullLotAlwaysLetsACarOut() {
        assertEquals(1000, count(EXIT, 24));
    }

    @Test
    void theLotDriftsTowardHalfToSeventyPercent() {
        assertTrue(count(ENTER, 6) > 700);
        assertTrue(count(EXIT, 20) > 700);
    }

    @Test
    void aCarWithoutPermitNeverGetsAnAccessibleSpot() {
        assertEquals(Optional.empty(), SimulatorPolicy.chooseSpot(Set.of("A01", "A02"), false, rnd));
        assertEquals(Optional.empty(), SimulatorPolicy.chooseSpot(Set.of(), false, rnd));
        assertEquals(Optional.of("B07"), SimulatorPolicy.chooseSpot(Set.of("A01", "B07"), false, rnd));
    }

    @Test
    void aCarWithPermitPrefersAnAccessibleSpot() {
        assertEquals(Optional.of("A02"), SimulatorPolicy.chooseSpot(Set.of("A02", "B07"), true, rnd));
        assertEquals(Optional.of("B07"), SimulatorPolicy.chooseSpot(Set.of("B07"), true, rnd));
    }

    @Test
    void delaysAreThreeToEightSeconds() {
        for (int i = 0; i < 500; i++) {
            long ms = SimulatorPolicy.nextDelay(rnd).toMillis();
            assertTrue(ms >= 3000 && ms <= 8000, ms + " ms");
        }
    }

    @Test
    void onlyCarsThatStayedTwentyMinutesLeaveOtherwiseTheOldest() {
        Instant now = Instant.parse("2026-10-27T12:00:00Z");
        ParkedCarDto old = parked("A05", now.minus(Duration.ofMinutes(25)));
        ParkedCarDto fresh = parked("B02", now.minus(Duration.ofMinutes(5)));
        ParkedCarDto fresher = parked("B03", now.minus(Duration.ofMinutes(1)));

        for (int i = 0; i < 50; i++) {
            assertEquals("A05", SimulatorPolicy.pickLeaving(List.of(fresh, old, fresher), now, rnd));
        }
        assertEquals("B02", SimulatorPolicy.pickLeaving(List.of(fresher, fresh), now, rnd));
    }

    private static ParkedCarDto parked(String spot, Instant enteredAt) {
        return new ParkedCarDto(spot, "SB-01-ABC", "code", "PP-00001", CarModel.SEDAN, "#123456", false, enteredAt);
    }
}
