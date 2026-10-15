package com.japtrack.project.controller;

import com.japtrack.project.entity.Application;
import com.japtrack.project.entity.User;
import com.japtrack.project.repository.ApplicationRepository;
import com.japtrack.project.repository.UserRepository;
import jakarta.servlet.http.Cookie;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end checks for /api/users/me: two real users, real logins (session cookies) and real CSRF tokens.
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileTest {

    private static final String PASSWORD = "correct-horse-battery";

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
        alice = saveUser("alice");
        bob = saveUser("bob");
        aliceSession = login("alice");
    }

    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }


    // HELPERS

    private User saveUser(String userName) {
        User user = new User();
        user.setUserName(userName);
        user.setUserEmail(userName + "@example.com");
        user.setUserFirstName(userName);
        user.setUserLastName("Tester");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return userRepository.save(user);
    }

    private Application saveApplication(User owner, String companyName) {
        Application application = new Application();
        application.setUser(owner);
        application.setCompanyName(companyName);
        application.setPositionTitle("Intern");
        application.setDateApplied(LocalDate.of(2026, 9, 1));
        return applicationRepository.save(application);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(new MockHttpSession())
                        .param("username", username)
                        .param("password", PASSWORD)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private ResultActions patchMe(String json) throws Exception {
        return mockMvc.perform(patch("/api/users/me")
                .session(aliceSession)
                .with(validToken(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private User reload(User user) {
        return userRepository.findById(user.getUserId()).orElseThrow();
    }


    // GET /api/users/me

    @Test
    void getMeWithoutLoginReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeReturnsTheLoggedInUsersProfile() throws Exception {
        mockMvc.perform(get("/api/users/me").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(alice.getUserId()))
                .andExpect(jsonPath("$.userName").value("alice"))
                .andExpect(jsonPath("$.userEmail").value("alice@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }


    // The old /api/users/{userId} routes are gone

    @Test
    void oldGetByIdRouteNoLongerWorks() throws Exception {
        mockMvc.perform(get("/api/users/{id}", bob.getUserId()).session(aliceSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void oldPatchByIdRouteNoLongerWorks() throws Exception {
        mockMvc.perform(patch("/api/users/{id}", bob.getUserId())
                        .session(aliceSession)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userFirstName\": \"Hacked\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        assertThat(reload(bob).getUserFirstName()).isEqualTo("bob");
    }

    @Test
    void oldDeleteByIdRouteNoLongerWorks() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", bob.getUserId())
                        .session(aliceSession)
                        .with(validToken(mockMvc)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        assertThat(userRepository.existsById(bob.getUserId())).isTrue();
    }


    // PATCH /api/users/me

    @Test
    void updateOwnFirstAndLastName() throws Exception {
        patchMe("{\"userFirstName\": \"Alicia\", \"userLastName\": \"Smith\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userFirstName").value("Alicia"))
                .andExpect(jsonPath("$.userLastName").value("Smith"))
                // Fields that weren't sent stay the same
                .andExpect(jsonPath("$.userName").value("alice"))
                .andExpect(jsonPath("$.userEmail").value("alice@example.com"));

        assertThat(reload(alice).getUserFirstName()).isEqualTo("Alicia");
    }

    @Test
    void updateOwnUsername() throws Exception {
        patchMe("{\"userName\": \"alice2\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("alice2"));

        assertThat(reload(alice).getUserName()).isEqualTo("alice2");
    }

    @Test
    void keepingOwnUsernameSucceeds() throws Exception {
        patchMe("{\"userName\": \"alice\", \"userFirstName\": \"Alicia\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("alice"));
    }

    @Test
    void updateOwnEmail() throws Exception {
        patchMe("{\"userEmail\": \"alice.new@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("alice.new@example.com"));

        // The new email works for logging in
        login("alice.new@example.com");
    }

    @Test
    void emailIsTrimmedAndStoredLowercase() throws Exception {
        patchMe("{\"userEmail\": \"  Alice.New@Example.COM \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("alice.new@example.com"));

        assertThat(reload(alice).getUserEmail()).isEqualTo("alice.new@example.com");
    }

    @Test
    void keepingOwnEmailSucceedsEvenInADifferentCase() throws Exception {
        patchMe("{\"userEmail\": \"Alice@Example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("alice@example.com"));
    }

    @Test
    void anotherUsersUsernameReturns409() throws Exception {
        patchMe("{\"userName\": \"bob\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("@username already in use, please try another one."));

        assertThat(reload(alice).getUserName()).isEqualTo("alice");
    }

    @Test
    void anotherUsersEmailReturns409() throws Exception {
        patchMe("{\"userEmail\": \"BOB@example.com\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use, please try another one."));

        assertThat(reload(alice).getUserEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void invalidEmailReturns400() throws Exception {
        patchMe("{\"userEmail\": \"not-an-email\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userEmail").value("Email must be valid"));
    }

    @Test
    void blankSuppliedFieldsReturn400() throws Exception {
        patchMe("{\"userName\": \"\", \"userFirstName\": \"  \", \"userLastName\": \" \", \"userEmail\": \"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.userName").value("Username cannot be blank"))
                .andExpect(jsonPath("$.fieldErrors.userFirstName").value("First name cannot be blank"))
                .andExpect(jsonPath("$.fieldErrors.userLastName").value("Last name cannot be blank"))
                .andExpect(jsonPath("$.fieldErrors.userEmail").exists());

        User unchanged = reload(alice);
        assertThat(unchanged.getUserName()).isEqualTo("alice");
        assertThat(unchanged.getUserEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void profilePatchCannotChangeThePassword() throws Exception {
        String hashBefore = reload(alice).getPasswordHash();

        patchMe("{\"password\": \"a-brand-new-password\", \"userFirstName\": \"Alicia\"}")
                .andExpect(status().isOk());

        // The password field was ignored: same hash, the old password still works, the new one doesn't
        assertThat(reload(alice).getPasswordHash()).isEqualTo(hashBefore);
        login("alice");
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "alice")
                        .param("password", "a-brand-new-password")
                        .with(validToken(mockMvc)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profilePatchCannotChooseAnotherUserId() throws Exception {
        patchMe("{\"userId\": " + bob.getUserId() + ", \"userFirstName\": \"Changed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(alice.getUserId()));

        // Only Alice changed; Bob is untouched
        assertThat(reload(alice).getUserFirstName()).isEqualTo("Changed");
        assertThat(reload(bob).getUserFirstName()).isEqualTo("bob");
    }


    // DELETE /api/users/me

    @Test
    void deletingOwnAccountReturns204AndRemovesIt() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .session(aliceSession)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(alice.getUserId())).isFalse();
    }

    @Test
    void deletingOwnAccountAlsoDeletesOwnApplications() throws Exception {
        Application first = saveApplication(alice, "Acme");
        Application second = saveApplication(alice, "Globex");

        mockMvc.perform(delete("/api/users/me")
                        .session(aliceSession)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent());

        assertThat(applicationRepository.existsById(first.getApplicationId())).isFalse();
        assertThat(applicationRepository.existsById(second.getApplicationId())).isFalse();
    }

    @Test
    void deletingOneAccountLeavesOtherUsersAndTheirApplications() throws Exception {
        saveApplication(alice, "Acme");
        Application bobsApplication = saveApplication(bob, "Initech");

        mockMvc.perform(delete("/api/users/me")
                        .session(aliceSession)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(bob.getUserId())).isTrue();
        assertThat(applicationRepository.existsById(bobsApplication.getApplicationId())).isTrue();

        // Bob can still log in and see his application
        MockHttpSession bobSession = login("bob");
        mockMvc.perform(get("/api/applications").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(bobsApplication.getApplicationId()));
    }

    @Test
    void deletingOwnAccountLogsOutTheSession() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/users/me")
                        .session(aliceSession)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent())
                .andReturn();

        // The session was invalidated on the server and the browser is told to drop the cookie
        assertThat(aliceSession.isInvalid()).isTrue();
        Cookie sessionCookie = result.getResponse().getCookie("JSESSIONID");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.getMaxAge()).isZero();

        // Sending the old session again gets 401, not 404, so it is no longer treated as logged in
        mockMvc.perform(get("/api/users/me").session(aliceSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/applications").session(aliceSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotDeleteAnotherUserThroughAnyPublicRoute() throws Exception {
        Application bobsApplication = saveApplication(bob, "Initech");

        // The old ID-based route
        mockMvc.perform(delete("/api/users/{id}", bob.getUserId())
                        .session(aliceSession)
                        .with(validToken(mockMvc)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        // /me with Bob's ID as a query parameter or in the body: the ID is ignored, so this deletes Alice only
        mockMvc.perform(delete("/api/users/me")
                        .param("userId", bob.getUserId().toString())
                        .session(aliceSession)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + bob.getUserId() + "}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(alice.getUserId())).isFalse();
        assertThat(userRepository.existsById(bob.getUserId())).isTrue();
        assertThat(applicationRepository.existsById(bobsApplication.getApplicationId())).isTrue();
    }
}
