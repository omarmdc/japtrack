package com.japtrack.project.controller;

import com.japtrack.project.entity.Application;
import com.japtrack.project.entity.User;
import com.japtrack.project.repository.ApplicationRepository;
import com.japtrack.project.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static com.japtrack.project.support.SpaCsrf.validToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end checks for PATCH /api/users/me/password: real users, real logins and real CSRF tokens.
@SpringBootTest
@AutoConfigureMockMvc
class PasswordChangeTest {

    private static final String OLD_PASSWORD = "correct-horse-battery";
    private static final String NEW_PASSWORD = "brand-new-password-42";
    private static final String BOB_PASSWORD = "bobs-own-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User alice;
    private User bob;
    private MockHttpSession aliceSession;

    @BeforeEach
    void createUsersAndLogIn() throws Exception {
        alice = saveUser("alice", OLD_PASSWORD);
        bob = saveUser("bob", BOB_PASSWORD);
        aliceSession = login("alice", OLD_PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }


    // HELPERS

    private User saveUser(String userName, String password) {
        User user = new User();
        user.setUserName(userName);
        user.setUserEmail(userName + "@example.com");
        user.setUserFirstName(userName);
        user.setUserLastName("Tester");
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private ResultActions attemptLogin(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .session(new MockHttpSession())
                .param("username", username)
                .param("password", password)
                .with(validToken(mockMvc)));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = attemptLogin(username, password).andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static String passwordJson(String currentPassword, String newPassword) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(currentPassword, newPassword);
    }

    private ResultActions changeAlicesPassword(String json) throws Exception {
        return mockMvc.perform(patch("/api/users/me/password")
                .session(aliceSession)
                .with(validToken(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String storedHash(User user) {
        return userRepository.findById(user.getUserId()).orElseThrow().getPasswordHash();
    }


    // Access rules

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordJson(OLD_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequestWithoutCsrfTokenReturns403() throws Exception {
        String hashBefore = storedHash(alice);

        mockMvc.perform(patch("/api/users/me/password")
                        .session(aliceSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordJson(OLD_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isForbidden());

        assertThat(storedHash(alice)).isEqualTo(hashBefore);
    }


    // Successful change

    @Test
    void validChangeReturns204AndStoresAnEncodedHash() throws Exception {
        changeAlicesPassword(passwordJson(OLD_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isNoContent());

        String hash = storedHash(alice);
        assertThat(hash).startsWith("{bcrypt}").doesNotContain(NEW_PASSWORD);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, hash)).isTrue();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, hash)).isFalse();
    }

    @Test
    void afterChangeOnlyTheNewPasswordLogsIn() throws Exception {
        changeAlicesPassword(passwordJson(OLD_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isNoContent());

        attemptLogin("alice", OLD_PASSWORD).andExpect(status().isUnauthorized());
        attemptLogin("alice", NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void currentSessionStillWorksAfterChange() throws Exception {
        changeAlicesPassword(passwordJson(OLD_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("alice"));
    }

    @Test
    void profileAndApplicationsAreNotChanged() throws Exception {
        Application application = new Application();
        application.setUser(alice);
        application.setCompanyName("Acme");
        application.setPositionTitle("Intern");
        application.setDateApplied(LocalDate.of(2026, 9, 1));
        applicationRepository.save(application);

        changeAlicesPassword(passwordJson(OLD_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isNoContent());

        User after = userRepository.findById(alice.getUserId()).orElseThrow();
        assertThat(after.getUserName()).isEqualTo("alice");
        assertThat(after.getUserEmail()).isEqualTo("alice@example.com");
        assertThat(after.getUserFirstName()).isEqualTo("alice");
        assertThat(after.getUserLastName()).isEqualTo("Tester");
        assertThat(applicationRepository.findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(alice.getUserId()))
                .singleElement()
                .extracting(Application::getCompanyName)
                .isEqualTo("Acme");
    }


    // Wrong current password

    @Test
    void wrongCurrentPasswordReturns400AndChangesNothing() throws Exception {
        String hashBefore = storedHash(alice);

        String body = changeAlicesPassword(passwordJson("not-my-password", NEW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"))
                .andReturn().getResponse().getContentAsString();

        // Nothing sensitive in the response, and the stored hash is untouched
        assertThat(body).doesNotContain("bcrypt").doesNotContain("not-my-password").doesNotContain(NEW_PASSWORD);
        assertThat(storedHash(alice)).isEqualTo(hashBefore);

        // The session is still valid: a wrong password here is bad input, not a logout
        mockMvc.perform(get("/api/users/me").session(aliceSession)).andExpect(status().isOk());
    }


    // Validation

    @Test
    void blankCurrentPasswordReturns400() throws Exception {
        changeAlicesPassword(passwordJson("   ", NEW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").value("Current password is required"));
    }

    @Test
    void blankNewPasswordReturns400() throws Exception {
        changeAlicesPassword(passwordJson(OLD_PASSWORD, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        changeAlicesPassword("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").value("Current password is required"))
                .andExpect(jsonPath("$.fieldErrors.newPassword").value("New password is required"));
    }

    @Test
    void tooShortNewPasswordReturns400() throws Exception {
        String hashBefore = storedHash(alice);

        changeAlicesPassword(passwordJson(OLD_PASSWORD, "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").value("Password must be between 8 and 72 characters"));

        assertThat(storedHash(alice)).isEqualTo(hashBefore);
    }

    @Test
    void tooLongNewPasswordReturns400() throws Exception {
        // 73 ASCII characters breaks both the character and the byte rule; either message may be the one reported
        changeAlicesPassword(passwordJson(OLD_PASSWORD, "a".repeat(73)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").value(anyOf(
                        equalTo("Password must be between 8 and 72 characters"),
                        equalTo("Password must be at most 72 bytes"))));
    }

    @Test
    void newPasswordOf72AsciiCharactersIsAccepted() throws Exception {
        String ascii72 = "a".repeat(72);

        changeAlicesPassword(passwordJson(OLD_PASSWORD, ascii72))
                .andExpect(status().isNoContent());

        assertThat(passwordEncoder.matches(ascii72, storedHash(alice))).isTrue();
    }

    @Test
    void newPasswordUnder72CharactersButOver72BytesReturns400AndKeepsTheOldHash() throws Exception {
        String hashBefore = storedHash(alice);

        // 37 characters but 74 UTF-8 bytes ("é" is 2 bytes): rejected by validation, never sent to BCrypt
        changeAlicesPassword(passwordJson(OLD_PASSWORD, "é".repeat(37)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.newPassword").value("Password must be at most 72 bytes"));

        assertThat(storedHash(alice)).isEqualTo(hashBefore);
        attemptLogin("alice", OLD_PASSWORD).andExpect(status().isNoContent());
    }


    // Only the logged-in user's password changes

    @Test
    void bodyCannotChooseAnotherUserId() throws Exception {
        String bobHashBefore = storedHash(bob);
        String json = """
                {
                  "userId": %d,
                  "currentPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(bob.getUserId(), OLD_PASSWORD, NEW_PASSWORD);

        changeAlicesPassword(json).andExpect(status().isNoContent());

        // The userId was ignored: Alice's password changed, Bob's did not
        assertThat(passwordEncoder.matches(NEW_PASSWORD, storedHash(alice))).isTrue();
        assertThat(storedHash(bob)).isEqualTo(bobHashBefore);
    }

    @Test
    void changingAlicesPasswordDoesNotChangeBobs() throws Exception {
        String bobHashBefore = storedHash(bob);

        changeAlicesPassword(passwordJson(OLD_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isNoContent());

        assertThat(storedHash(bob)).isEqualTo(bobHashBefore);
        attemptLogin("bob", BOB_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void profileUpdateStillCannotChangeThePassword() throws Exception {
        String hashBefore = storedHash(alice);

        mockMvc.perform(patch("/api/users/me")
                        .session(aliceSession)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + NEW_PASSWORD + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        assertThat(storedHash(alice)).isEqualTo(hashBefore);
        attemptLogin("alice", NEW_PASSWORD).andExpect(status().isUnauthorized());
    }
}
