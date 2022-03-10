package com.ckontur.edms.component.auth;

import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {
    public String generate() {
        /*
        return (new Random()).ints(48, 123)
            .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
            .limit(8)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
         */
        return "admin123";
    }
}
