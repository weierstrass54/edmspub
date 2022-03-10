package com.ckontur.edms.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticateRequest {
    @NotEmpty(message = "Поле login должно быть непустым.")
    private String login;

    @NotEmpty(message = "Поле password должно быть непустым.")
    private String password;
}
