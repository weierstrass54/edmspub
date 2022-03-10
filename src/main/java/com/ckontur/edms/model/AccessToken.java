package com.ckontur.edms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.collection.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccessToken {
    private User user;
    private Set<Permission> permissions;

    @JsonIgnore
    public UsernamePasswordAuthenticationToken toSecurityToken() {
        return new UsernamePasswordAuthenticationToken(user, "", permissions.toJavaSet());
    }
}
