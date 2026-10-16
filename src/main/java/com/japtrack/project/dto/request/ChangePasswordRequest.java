package com.japtrack.project.dto.request;

import com.japtrack.project.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// PATCH /api/users/me/password body.
// No userId (the account is always the logged-in one) and no confirmPassword (the frontend checks the user typed
// the new password twice; the server only needs the final value).
@Data
@NoArgsConstructor
public class ChangePasswordRequest {

    // Both fields are excluded from Lombok's toString(), so a raw password can't end up in a log line
    // just because this object was logged or printed in an error message
    @ToString.Exclude
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    // Same rules as registration (RegisterRequest), including the 72-byte limit BCrypt needs (see @MaxUtf8Bytes)
    @ToString.Exclude
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @MaxUtf8Bytes(value = 72, message = "Password must be at most 72 bytes")
    private String newPassword;
}
