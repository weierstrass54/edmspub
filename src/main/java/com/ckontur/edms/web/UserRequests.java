package com.ckontur.edms.web;

import com.ckontur.edms.model.Permission;
import com.ckontur.edms.validator.AtLeastOneNotEmpty;
import com.ckontur.edms.validator.Login;
import com.ckontur.edms.validator.Password;
import com.ckontur.edms.validator.Phone;
import io.vavr.collection.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

public class UserRequests {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateUser {
        @Login
        private String login;
        @Password
        private String password;
        @NotEmpty(message = "Поле firstName должно быть непустым.")
        private String firstName;
        @NotEmpty(message = "Поле middleName должно быть непустым.")
        private String middleName;
        @NotEmpty(message = "Поле lastName должно быть непустым.")
        private String lastName;
        @NotEmpty(message = "Поле appointment должно быть непустым.")
        private String appointment;
        @Phone
        private String phone;
        @Email @NotEmpty(message = "Поле email должно быть непустым.")
        private String email;
        private Set<Permission> permissions;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @AtLeastOneNotEmpty
    public static class UpdateUser {
        @Login(nullable = true)
        private String login;
        @Password(nullable = true)
        private String password;
        private String firstName;
        private String middleName;
        private String lastName;
        private String appointment;
        @Phone
        private String phone;
        @Email
        private String email;
        private Set<Permission> permissions;
    }
}
