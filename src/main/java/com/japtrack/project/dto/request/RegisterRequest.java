package com.japtrack.project.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // BCrypt only uses the first 72 bytes of a password
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    // Trim before validation runs, so " jane@example.com " is accepted as a valid email
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail == null ? null : userEmail.trim();
    }
}
