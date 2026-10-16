package com.japtrack.project.exception.custom;

// Thrown when a logged-in user's "current password" doesn't match during a password change.
// Mapped to 400 (not 401): the request is authenticated, only its input is wrong. A 401 would tell the
// frontend the session had ended. It's a plain RuntimeException rather than a Spring Security
// AuthenticationException on purpose, so it can't be mistaken for a failed login.
public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException (String message) {
        super(message);
    }
}
