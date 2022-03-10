package com.ckontur.edms.component.web;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.springframework.core.convert.converter.Converter;

public class StringToVavrSetLongConverter implements Converter<String, Set<Long>> {
    @Override
    public Set<Long> convert(String source) {
        return Option.of(source)
            .map(s ->
                HashSet.of(s.split(","))
                    .map(String::trim)
                    .map(Long::valueOf)
            )
            .getOrNull();
    }
}
