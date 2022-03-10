package com.ckontur.edms.web;

import com.ckontur.edms.exception.InvalidEnumException;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PageRequest {
    private final int page;
    private final int size;
    private final Direction direction;

    public static PageRequest of(Integer page, Integer size, Direction direction) {
        return new PageRequest(
            Option.of(page).filter(p -> p > 0).getOrElse(1),
            Option.of(size).filter(s -> s > 0).getOrElse(50),
            Option.of(direction).getOrElse(Direction.ASC)
        );
    }

    public int getOffset() {
        return (page - 1) * size;
    }

    public enum Direction {
        ASC, DESC;

        public static Direction of(String value) {
            return Stream.of(values()).filter(
                d -> d.name().equals(value.toUpperCase())
            ).getOrElseThrow(() -> new InvalidEnumException("Сортировка допускает только 'ACS' и 'DESC' значения"));
        }
    }
}
