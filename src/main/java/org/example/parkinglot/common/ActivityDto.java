package org.example.parkinglot.common;

import java.time.Instant;

/**
 * One line of the activity list: a car entered or left.
 *
 * @param type      "entered" or "exited"
 * @param amountBani what the car paid, only for "exited"
 */
public record ActivityDto(String type, String plate, String spot, String ticketCode, Integer amountBani,
                          Instant at) {
    public static final String ENTERED = "entered";
    public static final String EXITED = "exited";
}
