package org.example.parkinglot.common;

/** The numbers on the cards of the live page. Amounts are in bani. */
public record LotStatsDto(int capacity, int free, int revenueTodayBani, int revenueYesterdaySameTimeBani,
                          int carsToday, long averageStayMinutes) {
}
