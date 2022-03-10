package com.ckontur.edms.component.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.ckontur.edms.component.crypto.Cryptographer;
import com.ckontur.edms.model.AccessToken;
import com.ckontur.edms.model.VerifyToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static io.vavr.API.*;
import static io.vavr.Patterns.*;

@Slf4j
@Component
public class JwtVerifier {
    private final Cryptographer cryptographer;
    private final ObjectMapper objectMapper;
    private final JWTVerifier jwtVerifier;

    @Autowired
    public JwtVerifier(Algorithm algorithm, ObjectMapper objectMapper, Cryptographer cryptographer) {
        jwtVerifier = JWT.require(algorithm)
            .withIssuer("com.ckontur.auth2")
            .build();
        this.objectMapper = objectMapper;
        this.cryptographer = cryptographer;
    }

    public Option<? extends AccessToken> verify(String token) {
        Try<? extends AccessToken> verified = Try.of(() -> jwtVerifier.verify(token))
            .flatMap(jwt -> cryptographer.decrypt(jwt.getClaim("auth").asString()))
            .flatMap(this::parseToken);

        return Match(verified).of(
            Case($Success($()), Option::of),
            Case($Failure($()), e -> {
                log.error("Не удалось верифицировать токен: {}", e.getMessage(), e);
                return Option.none();
            })
        );
    }

    private Try<? extends AccessToken> parseToken(String token) {
        return Try.of(() -> objectMapper.readTree(token))
            .map(jn -> Option.of(jn.get("code")))
            .flatMap(jn -> Match(jn).of(
                Case($Some($()), __ -> Try.of(() -> objectMapper.readValue(token, VerifyToken.class))),
                Case($None(), () -> Try.of(() -> objectMapper.readValue(token, AccessToken.class)))
            ));
    }
}
