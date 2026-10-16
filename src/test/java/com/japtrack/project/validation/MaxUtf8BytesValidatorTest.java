package com.japtrack.project.validation;

import com.japtrack.project.dto.request.ChangePasswordRequest;
import com.japtrack.project.dto.request.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Runs the real Bean Validation engine against the two password DTOs, without starting Spring
class MaxUtf8BytesValidatorTest {

    private static final String BYTE_LIMIT_MESSAGE = "Password must be at most 72 bytes";

    // 72 plain ASCII characters = 72 bytes: the largest password BCrypt accepts
    private static final String ASCII_72 = "a".repeat(72);

    // 37 characters (passes @Size) but "é" is 2 bytes in UTF-8, so 74 bytes (too long for BCrypt)
    private static final String MULTIBYTE_37_CHARS_74_BYTES = "é".repeat(37);

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static RegisterRequest registerRequestWithPassword(String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUserName("jdoe");
        request.setUserFirstName("Jane");
        request.setUserLastName("Doe");
        request.setUserEmail("jdoe@example.com");
        request.setPassword(password);
        return request;
    }

    private static ChangePasswordRequest changeRequestWithNewPassword(String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-password");
        request.setNewPassword(newPassword);
        return request;
    }

    @Test
    void testStringsHaveTheExpectedSizes() {
        assertThat(ASCII_72).hasSize(72);
        assertThat(ASCII_72.getBytes(StandardCharsets.UTF_8)).hasSize(72);
        assertThat(MULTIBYTE_37_CHARS_74_BYTES).hasSize(37);
        assertThat(MULTIBYTE_37_CHARS_74_BYTES.getBytes(StandardCharsets.UTF_8)).hasSize(74);
    }


    // Registration

    @Test
    void registrationAccepts72AsciiCharacters() {
        assertThat(validator.validate(registerRequestWithPassword(ASCII_72))).isEmpty();
    }

    @Test
    void registrationRejectsPasswordOver72BytesEvenWhenUnder72Characters() {
        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(registerRequestWithPassword(MULTIBYTE_37_CHARS_74_BYTES));

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
            assertThat(violation.getMessage()).isEqualTo(BYTE_LIMIT_MESSAGE);
        });
    }


    // Password change

    @Test
    void passwordChangeAccepts72AsciiCharacters() {
        assertThat(validator.validate(changeRequestWithNewPassword(ASCII_72))).isEmpty();
    }

    @Test
    void passwordChangeRejectsNewPasswordOver72BytesEvenWhenUnder72Characters() {
        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(changeRequestWithNewPassword(MULTIBYTE_37_CHARS_74_BYTES));

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword");
            assertThat(violation.getMessage()).isEqualTo(BYTE_LIMIT_MESSAGE);
        });
    }


    // null is left to @NotBlank, so a missing password produces exactly one error, not two

    @Test
    void nullPasswordIsReportedOnlyByNotBlank() {
        assertThat(validator.validate(registerRequestWithPassword(null)))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Password is required");
    }
}
