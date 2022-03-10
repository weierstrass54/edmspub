package com.ckontur.edms.utils;

import io.vavr.control.Try;
import lombok.experimental.UtilityClass;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@UtilityClass
public class RSAUtils {
    private static final String PRIVATE_KEY_PREFIX = "-----BEGIN RSA PRIVATE KEY-----\n";
    private static final String PRIVATE_KEY_POSTFIX = "\n-----END RSA PRIVATE KEY-----\n";
    private static final String PUBLIC_KEY_PREFIX = "-----BEGIN RSA PUBLIC KEY-----\n";
    private static final String PUBLIC_KEY_POSTFIX = "\n-----END RSA PUBLIC KEY-----\n";
    private static final String X509_PREFIX = "-----BEGIN CERTIFICATE-----\n";
    private static final String X509_POSTFIX = "\n-----END CERTIFICATE-----\n";

    public static Try<RSAPrivateKey> readPrivateKeyFromResource(String name) {
        return getResource(name).flatMap(RSAUtils::parsePEMStream).flatMap(RSAUtils::getPrivateKey);
    }

    public static Try<RSAPublicKey> readPublicKeyFromResource(String name) {
        return getResource(name).flatMap(RSAUtils::parsePEMStream).flatMap(RSAUtils::getPublicKey);
    }

    public static String toString(PrivateKey key) {
        return PRIVATE_KEY_PREFIX + Base64.getEncoder().encodeToString(key.getEncoded()) + PRIVATE_KEY_POSTFIX;
    }

    public static String toString(PublicKey key) {
        return PUBLIC_KEY_PREFIX + Base64.getEncoder().encodeToString(key.getEncoded()) + PUBLIC_KEY_POSTFIX;
    }

    public static Try<String> toString(X509Certificate x509Certificate) {
        return Try.of(() -> X509_PREFIX + Base64.getEncoder().encodeToString(x509Certificate.getEncoded()) + X509_POSTFIX);
    }

    public static Try<X509Certificate> x509Of(String value) {
        return Try.of(() -> value.replace(X509_PREFIX, ""))
            .map(v -> v.replace(X509_POSTFIX, ""))
            .map(Base64.getDecoder()::decode)
            .flatMap(RSAUtils::getX509Certificate);
    }

    public static Try<RSAPrivateKey> privateKeyOf(String value) {
        return Try.of(() -> value.replace(PRIVATE_KEY_PREFIX, ""))
            .map(v -> v.replace(PRIVATE_KEY_POSTFIX, ""))
            .map(Base64.getDecoder()::decode)
            .flatMap(RSAUtils::getPrivateKey);
    }

    public static Try<RSAPublicKey> publicKeyOf(String value) {
        return Try.of(() -> value.replace(PUBLIC_KEY_PREFIX, ""))
            .map(v -> v.replace(PUBLIC_KEY_POSTFIX, ""))
            .map(Base64.getDecoder()::decode)
            .flatMap(RSAUtils::getPublicKey);
    }

    private static Try<byte[]> parsePEMStream(InputStream pemStream) {
        return Try.withResources(() -> new PemReader(new InputStreamReader(pemStream)))
            .of(PemReader::readPemObject)
            .map(PemObject::getContent);
    }

    private static Try<RSAPublicKey> getPublicKey(byte[] keyBytes) {
        return Try.of(() -> KeyFactory.getInstance("RSA"))
            .flatMap(kf -> {
                EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                return Try.of(() -> (RSAPublicKey) kf.generatePublic(spec));
            });
    }

    private static Try<RSAPrivateKey> getPrivateKey(byte[] keyBytes) {
        return Try.of(() -> KeyFactory.getInstance("RSA"))
            .flatMap(kf -> {
                EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                return Try.of(() -> (RSAPrivateKey) kf.generatePrivate(spec));
            });
    }

    private static Try<X509Certificate> getX509Certificate(byte[] x509Bytes) {
        return Try.of(() -> CertificateFactory.getInstance("X.509"))
            .flatMap(cf -> Try.of(() -> (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509Bytes))));
    }

    private static Try<InputStream> getResource(String name) {
        return Try.of(() -> RSAUtils.class.getClassLoader().getResourceAsStream(name));
    }

}
