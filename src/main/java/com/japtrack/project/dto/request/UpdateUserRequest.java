package com.japtrack.project.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

// PATCH /api/users/me body. Every field is optional and null means "leave unchanged".
// There is no userId (the account is always the logged-in one) and no password
// (password changes get their own endpoint that asks for the current password).
// Unknown JSON fields such as "userId" or "password" are ignored when the body is read.
@Data
@NoArgsConstructor
public class UpdateUserRequest {

    // @NotBlank would reject a missing field; this pattern only rejects a value that is present but blank
    @Pattern(regexp = ".*\\S.*", message = "Username cannot be blank")
    @Size(max = 255, message = "Username must be at most 255 characters")
    private String userName;

    @Pattern(regexp = ".*\\S.*", message = "First name cannot be blank")
    @Size(max = 255, message = "First name must be at most 255 characters")
    private String userFirstName;

    @Pattern(regexp = ".*\\S.*", message = "Last name cannot be blank")
    @Size(max = 255, message = "Last name must be at most 255 characters")
    private String userLastName;

    @Pattern(regexp = ".*\\S.*", message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String userEmail;

    // Trim before validation runs (same as RegisterRequest), so " jane@example.com " is a valid email.
    // Lowercasing happens in the service via User.normalizeEmail.
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail == null ? null : userEmail.trim();
    }
}
