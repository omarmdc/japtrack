package com.japtrack.project.exception.handler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.japtrack.project.support.SpaCsrf.validToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    // The full application, including Spring Security, with a logged-in user
    @Autowired
    private MockMvc mockMvc;

    // No @Valid endpoint exists yet and no real endpoint fails on purpose, so validation and
    // unexpected-error handling are checked against a small test-only controller.
    // Nested test classes are excluded from component scanning, so it never becomes part of the app.
    private final MockMvc testControllerMockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @RestController
    static class TestController {

        record Payload(@NotBlank(message = "Name is required") String name,
                       @Email(message = "Email must be valid") String email) {
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody Payload payload) {
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("internal detail that must not leak");
        }
    }

    private static final String VALID_APPLICATION_FIELDS =
            "\"userId\": 1, \"companyName\": \"Acme\", \"positionTitle\": \"Intern\"";


    // 400: wrong-typed parameter

    @Test
    void wrongTypedPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/api/users/abc").with(user("tester")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'userId'"))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    // 400: unreadable body

    @Test
    void unknownEnumValueReturns400() throws Exception {
        mockMvc.perform(post("/api/applications").with(user("tester")).with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + VALID_APPLICATION_FIELDS
                                + ", \"dateApplied\": \"2026-01-15\", \"status\": \"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void invalidDateReturns400() throws Exception {
        mockMvc.perform(post("/api/applications").with(user("tester")).with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + VALID_APPLICATION_FIELDS + ", \"dateApplied\": \"2026-13-45\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/applications").with(user("tester")).with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }


    // 400: validation

    @Test
    void validationFailureReturns400WithFieldErrors() throws Exception {
        testControllerMockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"));
    }

    @Test
    void fieldErrorsAreOmittedWhenThereAreNone() throws Exception {
        mockMvc.perform(get("/api/users/abc").with(user("tester")))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }


    // 404 and 405 are preserved

    @Test
    void unknownRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Not Found"));
    }

    @Test
    void testControllerIsNotPartOfTheApplication() throws Exception {
        mockMvc.perform(get("/test/boom").with(user("tester")))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingResourceStillReturns404WithItsMessage() throws Exception {
        mockMvc.perform(get("/api/users/999999").with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Sorry, we couldn't find user with ID: 999999"));
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(put("/api/users/1").with(user("tester")).with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("Method Not Allowed"));
    }


    // 500: genuine unexpected errors

    @Test
    void unexpectedErrorReturnsGeneric500AndIsLogged(CapturedOutput output) throws Exception {
        MvcResult result = testControllerMockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Sorry, something went wrong :("))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("internal detail");
        assertThat(output).contains("Unexpected error while handling request")
                .contains("internal detail that must not leak");
    }
}
