package com.japtrack.project.dto.request;

import com.japtrack.project.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String userName;

    @NotBlank(message = "First name is required")
    private String userFirstName;

    @NotBlank(message = "Last name is required")
    private String userLastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String userEmail;

    // Excluded from Lombok's toString() so the raw password can't end up in a log line.
    // BCrypt can't hash more than 72 bytes, and @Size counts characters (an "é" is 1 character but 2 bytes),
    // so @MaxUtf8Bytes adds the byte limit to keep oversized passwords a 400 instead of a 500 from encode().
    @ToString.Exclude
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @MaxUtf8Bytes(value = 72, message = "Password must be at most 72 bytes")
    private String password;

    // Trim before validation runs, so " jane@example.com " is accepted as a valid email
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail == null ? null : userEmail.trim();
    }
}
