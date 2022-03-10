package com.ckontur.edms.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignRequest {
    @NotEmpty(message = "Поле notes должно быть непустым.")
    private String notes;
}
