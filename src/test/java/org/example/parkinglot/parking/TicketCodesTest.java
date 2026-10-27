package org.example.parkinglot.parking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketCodesTest {

    @Test
    void codesAre22UrlSafeCharactersAndUnique() {
        String a = TicketCodes.newCode();
        String b = TicketCodes.newCode();

        assertTrue(a.matches("[A-Za-z0-9_-]{22}"));
        assertNotEquals(a, b);
        assertTrue(TicketCodes.looksValid(a));
        assertFalse(TicketCodes.looksValid("../etc"));
        assertFalse(TicketCodes.looksValid("x".repeat(500)));
        assertFalse(TicketCodes.looksValid(null));
    }

    @Test
    void displayNumbersHaveFiveDigits() {
        assertEquals("PP-00042", TicketCodes.displayNumber(42));
    }
}
