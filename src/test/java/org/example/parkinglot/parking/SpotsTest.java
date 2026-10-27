package org.example.parkinglot.parking;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpotsTest {

    @Test
    void thereAre24SpotsAndThreeAccessible() {
        assertEquals(24, Spots.ALL.size());
        assertEquals("A01", Spots.ALL.get(0));
        assertEquals("B12", Spots.ALL.get(23));
        assertEquals(Set.of("A01", "A02", "A03"), Spots.ACCESSIBLE);
        assertTrue(Spots.isValid("B07"));
        assertFalse(Spots.isValid("C01"));
        assertFalse(Spots.isValid(null));
    }
}
