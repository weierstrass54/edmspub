package com.ckontur.edms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.collection.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String login;
    @JsonIgnore
    private String password;
    private String firstName;
    private String middleName;
    private String lastName;
    private String appointment;
    private String phone;
    private String email;
    private Set<Permission> permissions;
}
