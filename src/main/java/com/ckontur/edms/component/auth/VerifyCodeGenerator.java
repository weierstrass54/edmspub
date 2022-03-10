package com.ckontur.edms.component.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class VerifyCodeGenerator {
    public String generate() {
        String code = String.format("%06d", (new Random()).nextInt(999999));
        log.info("Verification code: {}", code);
        return code;
    }
}
