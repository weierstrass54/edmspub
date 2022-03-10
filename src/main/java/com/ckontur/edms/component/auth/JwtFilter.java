package com.ckontur.edms.component.auth;

import com.ckontur.edms.model.AccessToken;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter implements Filter {
    private static final String AUTH_HEADER = "Authorization";

    @Autowired
    private JwtVerifier jwtVerifier;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        extractToken((HttpServletRequest) request)
            .flatMap(jwtVerifier::verify)
            .map(AccessToken::toSecurityToken)
            .forEach(SecurityContextHolder.getContext()::setAuthentication);
        chain.doFilter(request, response);
    }

    private Option<String> extractToken(HttpServletRequest request) {
        return Option.of(request.getHeader(AUTH_HEADER))
            .filter(bearer -> bearer.startsWith("Bearer "))
            .map(bearer -> bearer.substring(7));
    }

}
