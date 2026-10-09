package com.japtrack.project.controller;

import com.japtrack.project.entity.User;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.support.SpaCsrf;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.japtrack.project.support.SpaCsrf.validToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void deleteUsers() {
        userRepository.deleteAll();
    }

    private static String registerJson(String userName, String email, String password) {
        return """
                {
                  "userName": "%s",
                  "userFirstName": "Jane",
                  "userLastName": "Doe",
                  "userEmail": "%s",
                  "password": "%s"
                }
                """.formatted(userName, email, password);
    }

    private ResultActions register(String json) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .with(validToken(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(new MockHttpSession())
                        .param("username", username)
                        .param("password", password)
                        .with(validToken(mockMvc)))
                .andExpect(status().isNoContent())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }


    // Registration

    @Test
    void validRegistrationReturns201WithUser() throws Exception {
        register(registerJson("jdoe", "jdoe@example.com", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.userName").value("jdoe"))
                .andExpect(jsonPath("$.userEmail").value("jdoe@example.com"))
                .andExpect(jsonPath("$.userFirstName").value("Jane"))
                .andExpect(jsonPath("$.userLastName").value("Doe"))
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(userRepository.findByUserEmail("jdoe@example.com")).isPresent();
    }

    @Test
    void registrationResponseNeverContainsPasswordOrHash() throws Exception {
        String body = register(registerJson("jdoe", "jdoe@example.com", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(PASSWORD).doesNotContain("{bcrypt}");

        // Stored hashed, never as the raw password
        User stored = userRepository.findByUserEmail("jdoe@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
    }

    @Test
    void registrationStoresEmailTrimmedAndLowercaseAndLoginIgnoresCase() throws Exception {
        register(registerJson("jdoe", "  Jane.Doe@Example.COM ", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userEmail").value("jane.doe@example.com"));

        MockHttpSession session = login("JANE.DOE@example.com", PASSWORD);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("jane.doe@example.com"));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        register(registerJson("jdoe", "jdoe@example.com", PASSWORD)).andExpect(status().isCreated());

        // Same email in a different case still counts as taken
        register(registerJson("someone-else", "JDoe@Example.com", PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already in use, please try another one."));
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        register(registerJson("jdoe", "jdoe@example.com", PASSWORD)).andExpect(status().isCreated());

        register(registerJson("jdoe", "other@example.com", PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("@username already in use, please try another one."));
    }

    @Test
    void invalidRegistrationReturns400WithFieldErrors() throws Exception {
        String json = """
                {
                  "userName": "",
                  "userFirstName": " ",
                  "userEmail": "not-an-email",
                  "password": "short"
                }
                """;

        register(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.userName").value("Username is required"))
                .andExpect(jsonPath("$.fieldErrors.userFirstName").value("First name is required"))
                .andExpect(jsonPath("$.fieldErrors.userLastName").value("Last name is required"))
                .andExpect(jsonPath("$.fieldErrors.userEmail").value("Email must be valid"))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be between 8 and 72 characters"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void passwordLongerThan72CharactersReturns400() throws Exception {
        register(registerJson("jdoe", "jdoe@example.com", "a".repeat(73)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be between 8 and 72 characters"));
    }

    @Test
    void passwordOfExactly72CharactersIsAccepted() throws Exception {
        register(registerJson("jdoe", "jdoe@example.com", "a".repeat(72)))
                .andExpect(status().isCreated());
    }

    @Test
    void registrationWithoutCsrfTokenReturns403() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("jdoe", "jdoe@example.com", PASSWORD)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void registrationDoesNotLogTheUserIn() throws Exception {
        MvcResult result = register(registerJson("jdoe", "jdoe@example.com", PASSWORD))
                .andExpect(status().isCreated())
                .andReturn();

        // No session was created and no session cookie was issued
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getCookie("JSESSIONID")).isNull();

        // Sending back everything the registration response set still isn't a login
        Cookie[] cookies = result.getResponse().getCookies();
        var meRequest = get("/api/auth/me");
        if (cookies.length > 0) {
            meRequest.cookie(cookies);
        }
        mockMvc.perform(meRequest).andExpect(status().isUnauthorized());
    }


    // Current user

    @Test
    void meWithoutLoginReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void meAfterLoginReturnsTheLoggedInUser() throws Exception {
        register(registerJson("jdoe", "jdoe@example.com", PASSWORD)).andExpect(status().isCreated());
        Long userId = userRepository.findByUserEmail("jdoe@example.com").orElseThrow().getUserId();

        MockHttpSession session = login("jdoe", PASSWORD);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.userName").value("jdoe"))
                .andExpect(jsonPath("$.userEmail").value("jdoe@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meIgnoresAUserIdSentByTheClient() throws Exception {
        register(registerJson("jdoe", "jdoe@example.com", PASSWORD)).andExpect(status().isCreated());
        register(registerJson("other", "other@example.com", PASSWORD)).andExpect(status().isCreated());
        Long otherUserId = userRepository.findByUserEmail("other@example.com").orElseThrow().getUserId();

        MockHttpSession session = login("jdoe@example.com", PASSWORD);

        mockMvc.perform(get("/api/auth/me").session(session).param("userId", otherUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("jdoe"));
    }


    // CSRF token

    @Test
    void csrfEndpointIssuesAUsableToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(SpaCsrf.COOKIE_NAME);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isNotBlank();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        assertThat(result.getRequest().getSession(false)).isNull();

        // Using exactly that cookie and its value in the header gets a state-changing request through
        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header(SpaCsrf.HEADER_NAME, csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("jdoe", "jdoe@example.com", PASSWORD)))
                .andExpect(status().isCreated());
    }
}
