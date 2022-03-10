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
import java.util.regex.Pattern;

@Constraint(validatedBy = Password.PasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "Пароль должен иметь длину от {min} до {max} символов, содержать хотя бы одну букву и цифру и не содержать пробелов.";
    boolean nullable() default false;
    int min() default 6;
    int max() default 50;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class PasswordValidator implements ConstraintValidator<Password, String> {
        private static final Pattern DIGIT_REGEX = Pattern.compile("\\d");
        private static final Pattern DIGITS_ONLY_REGEX = Pattern.compile("^\\d+$");

        private boolean nullable;
        private int min;
        private int max;

        @Override
        public void initialize(Password constraintAnnotation) {
            this.nullable = constraintAnnotation.nullable();
            this.min = constraintAnnotation.min();
            this.max = constraintAnnotation.max();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return nullable;
            return Option.of(value)
                .filter(v -> v.length() >= min && v.length() <= max)
                .filter(v -> v.indexOf(' ') == -1)
                .filter(v -> !DIGITS_ONLY_REGEX.matcher(v).matches())
                .filter(v -> DIGIT_REGEX.matcher(v).find())
                .map(__ -> true)
                .getOrElse(false);
        }
    }

}
