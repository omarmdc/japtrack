package com.japtrack.project.security;

import com.japtrack.project.entity.User;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.support.SpaCsrf;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static com.japtrack.project.support.SpaCsrf.mismatchedToken;
import static com.japtrack.project.support.SpaCsrf.validToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void createUser() {
        user = new User();
        user.setUserName("jdoe");
        user.setUserEmail("jdoe@example.com");
        user.setUserFirstName("Jane");
        user.setUserLastName("Doe");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user = userRepository.save(user);
    }

    @AfterEach
    void deleteUsers() {
        userRepository.deleteAll();
    }

    private MvcResult login(String username, String password, MockHttpSession session) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .param("username", username)
                        .param("password", password)
                        .with(validToken(mockMvc)))
                .andReturn();
    }

    private MockHttpSession loggedInSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult result = login("jdoe@example.com", PASSWORD, session);
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        return (MockHttpSession) result.getRequest().getSession(false);
    }


    // Unauthenticated access

    @Test
    void protectedEndpointWithoutSessionReturnsJson401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/{userId}", user.getUserId()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();

        // No session should be created just because someone hit the API without logging in
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void noDefaultLoginPageIsGenerated() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }


    // Login

    @Test
    void loginWithEmailReturns204AndStoresAuthenticatedUserInSession() throws Exception {
        MvcResult result = login("jdoe@example.com", PASSWORD, new MockHttpSession());

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isEmpty();

        SecurityContext context = (SecurityContext) result.getRequest().getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context).isNotNull();
        AuthenticatedUser principal = (AuthenticatedUser) context.getAuthentication().getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(user.getUserId());
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void loginWithUsernameReturns204() throws Exception {
        MvcResult result = login("jdoe", PASSWORD, new MockHttpSession());

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void loginChangesTheSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String idBeforeLogin = session.getId();

        MvcResult result = login("jdoe@example.com", PASSWORD, session);

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(idBeforeLogin);
    }

    @Test
    void sessionFromLoginAuthenticatesLaterRequests() throws Exception {
        MockHttpSession session = loggedInSession();

        mockMvc.perform(get("/api/users/{userId}", user.getUserId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("jdoe@example.com"));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveIdenticalGeneric401() throws Exception {
        MvcResult wrongPassword = login("jdoe@example.com", "wrong-password", new MockHttpSession());
        MvcResult unknownUser = login("nobody@example.com", PASSWORD, new MockHttpSession());

        for (MvcResult result : new MvcResult[]{wrongPassword, unknownUser}) {
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(result.getResponse().getContentAsString())
                    .contains("\"status\":401")
                    .contains("\"message\":\"Invalid credentials\"");
        }
    }


    // CSRF

    @Test
    void csrfTokenIsIssuedAsReadableCookieWithoutCreatingSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(SpaCsrf.COOKIE_NAME);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isNotBlank();
        // The frontend must be able to read it, to copy it into the X-XSRF-TOKEN header
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        // Cookie-based CSRF must not need a server session
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void loginIssuesANewCsrfToken() throws Exception {
        Cookie tokenBeforeLogin = SpaCsrf.fetchToken(mockMvc);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(new MockHttpSession())
                        .param("username", "jdoe@example.com")
                        .param("password", PASSWORD)
                        .cookie(tokenBeforeLogin)
                        .header(SpaCsrf.HEADER_NAME, tokenBeforeLogin.getValue()))
                .andExpect(status().isNoContent())
                .andReturn();

        // The response first clears the old cookie, then sets the new token
        String newToken = Arrays.stream(result.getResponse().getCookies())
                .filter(cookie -> SpaCsrf.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isEmpty())
                .reduce((first, second) -> second)
                .orElse(null);
        assertThat(newToken).isNotNull().isNotEqualTo(tokenBeforeLogin.getValue());
    }

    @Test
    void authenticatedPatchWithValidCsrfTokenSucceeds() throws Exception {
        MockHttpSession session = loggedInSession();

        mockMvc.perform(patch("/api/users/{userId}", user.getUserId())
                        .session(session)
                        .with(validToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userFirstName\": \"Janet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userFirstName").value("Janet"));
    }

    @Test
    void loginWithMismatchedCsrfTokenReturnsJson403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "jdoe@example.com")
                        .param("password", PASSWORD)
                        .with(mismatchedToken(mockMvc)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
    }

    @Test
    void authenticatedPostWithMismatchedCsrfTokenReturnsJson403() throws Exception {
        MockHttpSession session = loggedInSession();

        mockMvc.perform(post("/api/applications")
                        .session(session)
                        .with(mismatchedToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
    }

    @Test
    void loginWithoutCsrfTokenReturnsJson403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "jdoe@example.com")
                        .param("password", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
    }

    @Test
    void authenticatedPostWithoutCsrfTokenReturnsJson403() throws Exception {
        MockHttpSession session = loggedInSession();

        mockMvc.perform(post("/api/applications")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
    }


    // Logout

    @Test
    void logoutReturns204InvalidatesSessionAndDeletesCookie() throws Exception {
        MockHttpSession session = loggedInSession();

        MvcResult result = mockMvc.perform(post("/api/auth/logout").session(session).with(validToken(mockMvc)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(session.isInvalid()).isTrue();
        Cookie sessionCookie = result.getResponse().getCookie("JSESSIONID");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.getMaxAge()).isZero();

        // A fresh request without the old session is unauthenticated again
        mockMvc.perform(get("/api/users/{userId}", user.getUserId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutCsrfTokenIsRejectedAndKeepsSession() throws Exception {
        MockHttpSession session = loggedInSession();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden());

        assertThat(session.isInvalid()).isFalse();
        mockMvc.perform(get("/api/users/{userId}", user.getUserId()).session(session))
                .andExpect(status().isOk());
    }
}
