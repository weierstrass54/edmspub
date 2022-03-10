package com.ckontur.edms.model;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SignRouteTemplate {
    private final Long id;
    private final String name;
    private final List<Long> userIds;
}
