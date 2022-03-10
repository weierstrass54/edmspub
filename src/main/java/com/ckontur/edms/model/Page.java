package com.ckontur.edms.model;

import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Page<T> {
    private final Traversable<T> items;
    private final int size;
    private final long total;

    public static <T> Page<T> of(Traversable<T> items, int size, Long total) {
        return new Page<>(items, size, total);
    }

    public static <T> Page<T> empty() {
        return new Page<>(List.empty(), 0, 0);
    }

    public long getTotalPages() {
        return total % size == 0 ? total / size : (total / size) + 1;
    }
}
