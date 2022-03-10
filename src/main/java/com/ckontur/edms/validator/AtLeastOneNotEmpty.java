package com.ckontur.edms.validator;

import io.vavr.Value;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.springframework.beans.BeanUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.beans.PropertyDescriptor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Map;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

@Constraint(validatedBy = AtLeastOneNotEmpty.AtLeastOneNotEmptyValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneNotEmpty {
    String message() default "По крайней мере одно поле должно быть непустым.";
    String[] fields() default {};
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class AtLeastOneNotEmptyValidator implements ConstraintValidator<AtLeastOneNotEmpty, Object> {
        private List<String> fields;

        @Override
        public void initialize(AtLeastOneNotEmpty constraintAnnotation) {
            this.fields = List.of(constraintAnnotation.fields());
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            boolean isInvalid = Try.sequence(
                getPropertyDescriptors(value).map(pd ->
                    Try.of(() -> pd.getReadMethod().invoke(value))
                        .map(Option::of)
                )
            ).map(f -> f
                .filter(Option::isDefined)
                .map(Option::get)
                .map(obj -> Match(obj).of(
                    Case($(instanceOf(CharSequence.class)), CharSequence::isEmpty),
                    Case($(instanceOf(Collection.class)), Collection::isEmpty),
                    Case($(instanceOf(Map.class)), Map::isEmpty),
                    Case($(instanceOf(Value.class)), Value::isEmpty),
                    Case($(instanceOf(Object[].class)), objs -> objs.length == 0),
                    Case($(), __ -> false)
                ))
            ).getOrElse(List.empty()).fold(true, (a, b) -> a && b);
            return !isInvalid;
        }

        private List<PropertyDescriptor> getPropertyDescriptors(Object value) {
            return Option.of(value).map(v ->
                List.of(BeanUtils.getPropertyDescriptors(v.getClass()))
                    .filter(pd -> !"class".equals(pd.getName()) && (fields.isEmpty() || fields.contains(pd.getName())))
            ).getOrElse(List.empty());
        }
    }
}
