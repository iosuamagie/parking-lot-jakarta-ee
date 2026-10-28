package org.example.parkinglot.common;

import java.time.LocalDate;

/** What was paid on one day (Romanian time). */
public record DayTotalDto(LocalDate day, int totalBani, int tickets) {
}
