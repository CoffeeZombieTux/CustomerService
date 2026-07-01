package io.customerservice.customerservice.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NotBlank
@Size(min = 8, max = 100)
@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "password must contain at least one uppercase letter and one digit")
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface ValidPassword {
    String message() default "invalid password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}