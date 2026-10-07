package com.japtrack.project.support;

import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// Sends CSRF tokens the same way the React frontend will: read the XSRF-TOKEN cookie the
// application issues, send the cookie back, and copy its value into the X-XSRF-TOKEN header.
//
// Used instead of Spring Security's csrf() test helper, which permanently swaps the app's
// cookie-based CSRF repository for a session-based one in the shared test context.
public final class SpaCsrf {

    public static final String COOKIE_NAME = "XSRF-TOKEN";
    public static final String HEADER_NAME = "X-XSRF-TOKEN";

    // Any response carries the cookie (csrf.spa() issues it eagerly); /api/auth/csrf is the
    // route the frontend is meant to call for it, so the tests use the same one
    private static final String TOKEN_URL = "/api/auth/csrf";

    private SpaCsrf() {
    }

    // Asks the application for a fresh XSRF-TOKEN cookie
    public static Cookie fetchToken(MockMvc mockMvc) throws Exception {
        Cookie cookie = mockMvc.perform(get(TOKEN_URL)).andReturn().getResponse().getCookie(COOKIE_NAME);
        assertThat(cookie).as("XSRF-TOKEN cookie issued by the application").isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        return cookie;
    }

    // A real token: the cookie plus the matching header
    public static RequestPostProcessor validToken(MockMvc mockMvc) throws Exception {
        Cookie cookie = fetchToken(mockMvc);
        return withCookieAndHeader(cookie, cookie.getValue());
    }

    // A real cookie, but a header value that doesn't match it
    public static RequestPostProcessor mismatchedToken(MockMvc mockMvc) throws Exception {
        Cookie cookie = fetchToken(mockMvc);
        return withCookieAndHeader(cookie, cookie.getValue() + "-tampered");
    }

    private static RequestPostProcessor withCookieAndHeader(Cookie cookie, String headerValue) {
        return request -> {
            List<Cookie> cookies = new ArrayList<>();
            if (request.getCookies() != null) {
                cookies.addAll(Arrays.asList(request.getCookies()));
            }
            cookies.add(cookie);
            request.setCookies(cookies.toArray(new Cookie[0]));
            request.addHeader(HEADER_NAME, headerValue);
            return request;
        };
    }
}
