package com.ckontur.edms.controller;

import com.ckontur.edms.exception.AuthenticationFailedException;
import com.ckontur.edms.model.User;
import com.ckontur.edms.service.AuthenticationService;
import com.ckontur.edms.web.AuthenticateRequest;
import com.ckontur.edms.web.ConfirmRequest;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(tags = {"Аутентификация"})
@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
@Timed(value = "requests.auth", percentiles = {0.75, 0.9, 0.95, 0.99})
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @GetMapping("/user")
    @PreAuthorize("hasAnyAuthority('VIEW', 'SIGN')")
    public User user(@AuthenticationPrincipal Authentication token) {
        return (User) token.getPrincipal();
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestBody @Valid AuthenticateRequest request) {
        return authenticationService.authenticate(request.getLogin(), request.getPassword())
            .getOrElseThrow(() -> new AuthenticationFailedException("Пара логин/пароль неверна."));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAuthority('AUTH_CONFIRM')")
    public String verify(@AuthenticationPrincipal Authentication token, @RequestBody @Valid ConfirmRequest request) {
        return authenticationService.confirm((User) token.getPrincipal(), (String) token.getCredentials(), request.getCode())
            .getOrElseThrow(() -> new AuthenticationFailedException("Код подтверждения неверен."));
    }

}
