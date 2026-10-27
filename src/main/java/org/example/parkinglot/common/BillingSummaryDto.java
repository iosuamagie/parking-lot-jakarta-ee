package org.example.parkinglot.common;

import java.time.LocalDate;
import java.util.List;

/**
 * The numbers of a billing period. {@code days} has every day of the period, also the days with nothing paid.
 */
public record BillingSummaryDto(LocalDate from, LocalDate to, int totalBani, int previousPeriodTotalBani,
                                int paidTickets, int averageBani, int freeTickets, List<DayTotalDto> days) {
}
