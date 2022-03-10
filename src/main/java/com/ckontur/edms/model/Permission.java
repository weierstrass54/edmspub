package com.ckontur.edms.model;

import com.ckontur.edms.exception.InvalidEnumException;
import io.vavr.collection.Stream;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public enum Permission implements GrantedAuthority {
    ADMIN, VIEW, SIGN, UPLOAD, DELETE, AUTH_CONFIRM;

    @Override
    public String getAuthority() {
        return name();
    }

    public static Permission of(String value) {
        return Stream.of(values())
            .filter(p -> p.name().equals(value.toUpperCase()))
            .getOrElseThrow(() -> new InvalidEnumException("Разрешения " + value + " не существует."));
    }
}
