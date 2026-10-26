package org.example.parkinglot.servlets.cars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddCarPhotoTest {

    @Test
    void keepsOnlyTheFileName() {
        assertEquals("car.png", AddCarPhoto.baseName("car.png"));
        assertEquals("car.png", AddCarPhoto.baseName("C:\\Users\\me\\Pictures\\car.png"));
        assertEquals("car.png", AddCarPhoto.baseName("/home/me/car.png"));
        assertEquals("passwd", AddCarPhoto.baseName("../../etc/passwd"));
    }

    @Test
    void usesADefaultNameWhenTheBrowserSendsNone() {
        assertEquals("photo", AddCarPhoto.baseName(null));
    }

    @Test
    void cutsVeryLongNames() {
        assertEquals(255, AddCarPhoto.baseName("a".repeat(1000)).length());
    }
}
