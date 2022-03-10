package com.ckontur.edms.utils;

import io.vavr.collection.*;
import io.vavr.control.Option;
import lombok.experimental.UtilityClass;

import java.sql.Array;
import java.sql.SQLException;
import java.util.function.Function;

@UtilityClass
public class SqlUtils {
    public static <T> List<T> listOf(Array value, Function<String, T> mapper) throws SQLException {
        return List.ofAll(streamOf(value, mapper));
    }

    public static <T> Set<T> setOf(Array value, Function<String, T> mapper) throws SQLException {
        return HashSet.ofAll(streamOf(value, mapper));
    }

    public static <T> String array(Traversable<T> values, Function<T, String> mapper) {
        return Option.of(values)
            .map(v -> "{" + v.map(mapper).mkString(",") + "}")
            .getOrNull();
    }

    private static <T> Stream<T> streamOf(Array value, Function<String, T> mapper) throws SQLException {
        return Stream.of((String[])value.getArray()).map(mapper);
    }

}
