package com.ckontur.edms.model;

import io.vavr.collection.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyToken extends AccessToken {
    private String code;

    public VerifyToken(User user, String code, Set<Permission> permissions) {
        super(user, permissions);
        this.code = code;
    }

    @Override
    public UsernamePasswordAuthenticationToken toSecurityToken() {
        return new UsernamePasswordAuthenticationToken(getUser(), code, getPermissions().toJavaSet());
    }
}
