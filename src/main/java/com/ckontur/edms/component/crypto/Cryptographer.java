package com.ckontur.edms.component.crypto;

import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class Cryptographer {
    @Value("${edms.aes.algorithm}")
    private String algorithm;

    @Value("${edms.aes.key}")
    private String key;

    @Value("${edms.aes.salt}")
    private String salt;

    private SecretKey secretKey;
    private IvParameterSpec iv;

    public Try<String> encrypt(String message) {
        return Try.of(() -> Cipher.getInstance(algorithm))
            .flatMap(cipher -> Try.of(() -> {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
                return cipher;
            }))
            .flatMap(cipher -> Try.of(() -> cipher.doFinal(message.getBytes())))
            .map(bytes -> Base64.getEncoder().encodeToString(bytes));
    }

    public Try<String> decrypt(String message) {
        return Try.of(() -> Cipher.getInstance(algorithm))
            .flatMap(cipher -> Try.of(() -> {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
                return cipher;
            }))
            .flatMap(cipher -> Try.of(() -> cipher.doFinal(Base64.getDecoder().decode(message))))
            .map(String::new);
    }

    @PostConstruct
    private void init() throws Exception {
        secretKey = Try.of(() -> SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512"))
            .flatMap(skf -> {
                KeySpec keySpec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), 131072, 256);
                return Try.of(() -> new SecretKeySpec(skf.generateSecret(keySpec).getEncoded(), "AES"));
            })
            .getOrElseThrow(t -> new Exception("Не удалось инициализировать алгоритм симметричного шифрования.", t));
        byte[] buffer = new byte[16];
        new SecureRandom().nextBytes(buffer);
        iv = new IvParameterSpec(buffer);
    }
}
