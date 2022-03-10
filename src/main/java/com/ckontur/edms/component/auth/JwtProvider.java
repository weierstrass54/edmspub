package com.ckontur.edms.component.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ckontur.edms.component.crypto.Cryptographer;
import com.ckontur.edms.exception.CryptoException;
import com.ckontur.edms.model.AccessToken;
import com.ckontur.edms.model.Permission;
import com.ckontur.edms.model.User;
import com.ckontur.edms.model.VerifyToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.HashSet;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final Cryptographer cryptographer;
    private final Algorithm algorithm;
    private final ObjectMapper objectMapper;

    public String generateConfirmToken(User user, String confirmationCode) {
        return JWT.create()
            .withIssuer("com.ckontur.auth2")
            .withClaim("auth", encryptConfirmToken(user, confirmationCode))
            .withExpiresAt(Date.from(LocalDateTime.now().plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant()))
            .sign(algorithm);
    }

    public String generateAccessToken(User user) {
        return JWT.create()
            .withIssuer("com.ckontur.auth2")
            .withClaim("auth", encryptAccessToken(user))
            .withExpiresAt(Date.from(LocalDateTime.now().plusMonths(1).atZone(ZoneId.systemDefault()).toInstant()))
            .sign(algorithm);
    }

    private String encryptConfirmToken(User user, String confirmationCode) {
        return Try.of(() ->
            objectMapper.writeValueAsString(new VerifyToken(user, confirmationCode, HashSet.of(Permission.AUTH_CONFIRM)))
        )
        .flatMap(cryptographer::encrypt)
        .getOrElseThrow(t -> new CryptoException("Не удалось зашифровать токен подтверждения.", t));
    }

    private String encryptAccessToken(User user) {
        return Try.of(() -> objectMapper.writeValueAsString(new AccessToken(user, user.getPermissions())))
            .flatMap(cryptographer::encrypt)
            .getOrElseThrow(t -> new CryptoException("Не удалось зашифровать токен доступа.", t));
    }

}
