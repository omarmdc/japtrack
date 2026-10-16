package com.japtrack.project.controller;

import com.japtrack.project.dto.request.ChangePasswordRequest;
import com.japtrack.project.dto.request.UpdateUserRequest;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.security.AuthenticatedUser;
import com.japtrack.project.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

// The logged-in user's own account. Accounts are created through POST /api/auth/register (AuthController).
//
// These routes used to be /api/users/{userId}, which let any logged-in user read, edit or delete any account
// (and, through the cascade, delete that account's applications). Now there is no userId in any route:
// @AuthenticationPrincipal gives us the AuthenticatedUser that Spring Security restored from the session,
// so the only account a request can touch is the one that logged in.
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // The same handlers Spring Security's own logout uses: invalidate the HTTP session, clear the
    // SecurityContext, and tell the browser to drop the session cookie
    private final LogoutHandler logoutHandler = new CompositeLogoutHandler(
            new SecurityContextLogoutHandler(),
            new CookieClearingLogoutHandler("JSESSIONID"));

    public UserController(UserService userService) {
        this.userService = userService;
    }


    // 1) Get the current user's profile
    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getCurrentUser(currentUser.getUserId());
    }

    // 2) Update the current user's profile (not the password)
    @PatchMapping("/me")
    public UserResponse updateCurrentUser(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                          @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateCurrentUser(currentUser.getUserId(), request);
    }

    // 3) Delete the current user's account, then log them out
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                  Authentication authentication,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        userService.deleteCurrentUser(currentUser.getUserId());

        // The account no longer exists, so the session that pointed to it must not keep working.
        // Logging out here (only after the delete succeeded) invalidates the session server-side,
        // so even a copy of the old session cookie is rejected with 401 afterwards.
        logoutHandler.logout(request, response, authentication);

        return ResponseEntity.noContent().build();
    }

    // 4) Change the current user's password (requires the current password)
    // Like the other /me routes, the account comes from the session principal, never from the request.
    // The session stays logged in afterwards: the AuthenticatedUser it holds has the user's ID and email but no
    // password (erased at login), so nothing in it becomes stale. Logging out the user's other browsers/devices
    // would need a session registry, which is out of scope for now.
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
