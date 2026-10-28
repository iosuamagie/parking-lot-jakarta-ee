package org.example.parkinglot.common;

import org.example.parkinglot.parking.CarModel;

import java.time.Instant;

/**
 * Everything the ticket card shows. {@code clockNow} and {@code clockSpeed} let the page keep the amount
 * of a parked car ticking with the parking clock.
 */
public record TicketDto(String ticketCode, String ticketNumber, String plate, String spot, CarModel model,
                        String color, boolean accessiblePermit, Instant enteredAt, Instant exitedAt,
                        Integer amountBani, Instant clockNow, int clockSpeed) {
}
