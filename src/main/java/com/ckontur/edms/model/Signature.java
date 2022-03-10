package com.ckontur.edms.model;

import com.ckontur.edms.utils.RSAUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.PublicKey;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class Signature {
    private final String notes;
    private final PublicKey key;
    //private final X509Certificate x509Certificate;
    private final LocalDateTime signedAt;

    public String getKey() {
        return RSAUtils.toString(key);
    }
}
