package org.example.parkinglot.servlets.cars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CarFormTest {

    @Test
    void acceptsAValidCar() {
        assertNull(CarForm.validateFormat("SB-12-ABC", "A01", 1L));
    }

    @Test
    void rejectsMissingOrTooShortFields() {
        assertNotNull(CarForm.validateFormat(null, "A01", 1L));
        assertNotNull(CarForm.validateFormat("SB-12-ABC", "", 1L));
        assertNotNull(CarForm.validateFormat("AB", "A01", 1L));
    }

    @Test
    void rejectsTooLongFields() {
        assertNotNull(CarForm.validateFormat("X".repeat(101), "A01", 1L));
    }

    @Test
    void requiresAnOwner() {
        assertNotNull(CarForm.validateFormat("SB-12-ABC", "A01", null));
    }
}
