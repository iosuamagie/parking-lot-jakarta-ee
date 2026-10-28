package org.example.parkinglot.common;

import java.util.List;

/** One page of a longer list; {@code page} starts at 1. */
public record PageDto<T>(List<T> items, int page, int pageSize, long total) {
    public int pages() {
        return (int) Math.max(1, (total + pageSize - 1) / pageSize);
    }
}
