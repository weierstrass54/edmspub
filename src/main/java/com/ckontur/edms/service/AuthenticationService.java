package com.ckontur.edms.service;

import com.ckontur.edms.component.DummySmsService;
import com.ckontur.edms.component.auth.JwtProvider;
import com.ckontur.edms.component.auth.VerifyCodeGenerator;
import com.ckontur.edms.model.User;
import com.ckontur.edms.repository.UserRepository;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final VerifyCodeGenerator verifyCodeGenerator;
    private final DummySmsService dummySmsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public Option<String> authenticate(String login, String password) {
        return userRepository.findByLogin(login)
            .filter(user -> passwordEncoder.matches(password, user.getPassword()))
            .map(user -> {
                String verificationCode = verifyCodeGenerator.generate();
                dummySmsService.sendVerificationCode(user.getPhone(), verificationCode);
                return jwtProvider.generateConfirmToken(user, passwordEncoder.encode(verificationCode));
            });
    }

    public Option<String> confirm(User user, String tokenCode, String rawCode) {
        return passwordEncoder.matches(rawCode, tokenCode) ?
            Option.of(jwtProvider.generateAccessToken(user)) : Option.none();
    }
}
