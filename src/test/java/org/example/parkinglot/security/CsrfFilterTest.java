package org.example.parkinglot.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrfFilterTest {

    @Test
    void acceptsTheSameToken() {
        assertTrue(CsrfFilter.matches("abc123", "abc123"));
    }

    @Test
    void rejectsADifferentToken() {
        assertFalse(CsrfFilter.matches("abc123", "abc124"));
        assertFalse(CsrfFilter.matches("abc123", "abc"));
    }

    @Test
    void rejectsAMissingToken() {
        assertFalse(CsrfFilter.matches("abc123", null));
        assertFalse(CsrfFilter.matches(null, "abc123"));
        assertFalse(CsrfFilter.matches(null, null));
    }
}
