package com.ckontur.edms.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SignRoute {
    private final Long id;
    private final User user;
    private final Integer ordinal;
    private final Signature signature;

    public boolean isSigned() {
        return signature != null;
    }
}
