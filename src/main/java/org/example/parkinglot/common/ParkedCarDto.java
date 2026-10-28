package org.example.parkinglot.common;

import org.example.parkinglot.parking.CarModel;

import java.time.Instant;

/** A car that is in the lot right now. */
public record ParkedCarDto(String spot, String plate, String ticketCode, String ticketNumber, CarModel model,
                           String color, boolean accessiblePermit, Instant enteredAt) {
}
