package com.japtrack.project.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Limits a String by its size in UTF-8 bytes, not characters.
//
// Used on passwords because BCrypt works on bytes: Spring Security's BCrypt refuses to hash a new password longer
// than 72 UTF-8 bytes (it throws IllegalArgumentException). @Size counts characters, and a character such as "é"
// or an emoji takes 2-4 bytes, so a password can pass @Size(max = 72) and still be too long for BCrypt.
// Checking bytes here rejects it as a normal 400 validation error, before PasswordEncoder.encode() is ever called.
//
// null is treated as valid; @NotBlank is responsible for missing values.
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "Must be at most {value} bytes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
