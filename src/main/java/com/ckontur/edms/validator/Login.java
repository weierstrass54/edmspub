package com.ckontur.edms.validator;

import io.vavr.control.Option;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = Login.LoginValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Login {
    String message() default "Логин не должен содержать пробелов и быть длиной от {min} до {max} символов.";
    boolean nullable() default false;
    int min() default 3;
    int max() default 50;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class LoginValidator implements ConstraintValidator<Login, String> {
        private int min;
        private int max;
        private boolean nullable;

        @Override
        public void initialize(Login constraintAnnotation) {
            this.min = constraintAnnotation.min();
            this.max = constraintAnnotation.max();
            this.nullable = constraintAnnotation.nullable();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return nullable;
            return Option.of(value)
                .filter(v -> v.length() >= min && v.length() <= max)
                .filter(v -> v.indexOf(' ') == -1)
                .map(__ -> true)
                .getOrElse(false);
        }
    }
}
