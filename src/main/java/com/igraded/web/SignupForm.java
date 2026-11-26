package com.igraded.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The fields on the sign-up page, with their rules. */
public class SignupForm {

    @NotBlank(message = "Enter your full name")
    @Size(max = 80, message = "Use 80 characters or fewer")
    private String fullName;

    @NotBlank(message = "Enter your email")
    @Email(message = "Enter a valid email address")
    @Size(max = 120, message = "Use 120 characters or fewer")
    private String email;

    // 72 is the most BCrypt can use
    @NotBlank(message = "Choose a password")
    @Size(min = 8, max = 72, message = "Use 8 to 72 characters")
    private String password;

    @NotBlank(message = "Type the password again")
    private String confirmPassword;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
