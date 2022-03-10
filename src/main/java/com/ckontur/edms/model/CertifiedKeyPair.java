package com.ckontur.edms.model;

import com.ckontur.edms.utils.RSAUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

@Getter
@RequiredArgsConstructor
@JsonSerialize(using = CertifiedKeyPair.CertifiedKeyPairSerializer.class)
public class CertifiedKeyPair {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final X509Certificate x509Certificate;

    public static class CertifiedKeyPairSerializer extends JsonSerializer<CertifiedKeyPair> {
        @Override
        public void serialize(CertifiedKeyPair value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            RSAUtils.toString(value.getX509Certificate()).flatMap(x509 -> Try.of(() -> {
                gen.writeStartObject();
                gen.writeStringField("privateKey", "**********");
                gen.writeStringField("publicKey", RSAUtils.toString(value.getPublicKey()));
                gen.writeStringField("x509Certificate", x509);
                gen.writeEndObject();
                return true;
            })).getOrElseThrow(() -> new IOException("Не удалось сериализовать ЭЦП."));
        }
    }
}
