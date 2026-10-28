package org.example.parkinglot.parking;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TariffTest {

    @Test
    void theFirstFifteenMinutesAreFree() {
        assertEquals(0, Tariff.amountBani(Duration.ofMinutes(15)));
    }

    @Test
    void everyStartedMinuteAfterThatCostsTenBani() {
        assertEquals(10, Tariff.amountBani(Duration.ofMinutes(15).plusSeconds(1)));
        assertEquals(10, Tariff.amountBani(Duration.ofMinutes(16)));
        assertEquals(20, Tariff.amountBani(Duration.ofMinutes(16).plusSeconds(1)));
        assertEquals(1030, Tariff.amountBani(Duration.ofMinutes(118)));
        assertEquals(0, Tariff.amountBani(Duration.ZERO));
    }

    @Test
    void amountsAreShownInRon() {
        assertEquals("10.30 RON", Tariff.format(1030));
        assertEquals("1,234.50 RON", Tariff.format(123450));
        assertEquals("0.00 RON", Tariff.format(0));
    }
}
