package org.example.parkinglot.common;

import java.time.Instant;

/** A ticket whose car has left and paid (0 bani for stays under 15 minutes). */
public record PaidTicketDto(String ticketCode, String ticketNumber, String plate, String spot, Instant enteredAt,
                            Instant exitedAt, int amountBani) {
}
