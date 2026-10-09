package com.japtrack.project.controller;

import com.japtrack.project.dto.request.RegisterRequest;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.security.AuthenticatedUser;
import com.japtrack.project.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login and logout are not here: Spring Security handles POST /api/auth/login and /api/auth/logout (see SecurityConfig)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }


    // 1) Register a new account (does not log the user in)
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2) The currently logged-in user, taken from the session, never from the client
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getUserById(currentUser.getUserId());
    }

    // 3) CSRF token for the frontend
    // The token is delivered as the XSRF-TOKEN cookie, which the frontend sends back in the
    // X-XSRF-TOKEN header. The body is empty on purpose: the token object Spring exposes here is
    // masked (BREACH protection) and would not match the cookie if sent in that header.
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        // Loading the token guarantees the cookie is written on this response
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }
}
