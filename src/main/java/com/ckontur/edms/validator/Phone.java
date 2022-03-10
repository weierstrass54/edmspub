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

@Constraint(validatedBy = Phone.PhoneValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {
    String message() default "Телефонный номер должен соответствовать шаблону +7XXXXXXXXXX.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class PhoneValidator implements ConstraintValidator<Phone, String> {
        private static final Pattern PHONE_REGEX = Pattern.compile("^\\+7\\d{10}$");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return Option.of(value).map(v -> PHONE_REGEX.matcher(v).matches()).getOrElse(true);
        }
    }
}
