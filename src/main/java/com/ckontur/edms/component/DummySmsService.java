package com.ckontur.edms.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DummySmsService {
    public void sendVerificationCode(String phone, String code) {
        log.info("Sent message {} on phone: {}", code, phone);
    }
}
