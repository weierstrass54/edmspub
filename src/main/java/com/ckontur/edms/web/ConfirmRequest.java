package com.ckontur.edms.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmRequest {
    @NotEmpty(message = "Поле code должно быть непустым.")
    private String code;
}
