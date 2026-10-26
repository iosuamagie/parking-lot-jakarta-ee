package org.example.parkinglot.servlets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParamsTest {

    @Test
    void parsesPositiveIds() {
        assertEquals(42L, Params.parseId("42"));
        assertEquals(7L, Params.parseId(" 7 "));
    }

    @Test
    void returnsNullForBadInput() {
        assertNull(Params.parseId(null));
        assertNull(Params.parseId(""));
        assertNull(Params.parseId("abc"));
        assertNull(Params.parseId("1; DROP TABLE car"));
        assertNull(Params.parseId("99999999999999999999"));
    }

    @Test
    void returnsNullForZeroAndNegativeIds() {
        assertNull(Params.parseId("0"));
        assertNull(Params.parseId("-5"));
    }
}
