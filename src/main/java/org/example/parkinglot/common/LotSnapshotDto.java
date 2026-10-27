package org.example.parkinglot.common;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Everything the live page needs to draw the lot from scratch. */
public record LotSnapshotDto(Instant clockNow, int clockSpeed, List<String> spots, Set<String> accessibleSpots,
                             List<ParkedCarDto> cars, LotStatsDto stats, List<ActivityDto> activity) {
}
