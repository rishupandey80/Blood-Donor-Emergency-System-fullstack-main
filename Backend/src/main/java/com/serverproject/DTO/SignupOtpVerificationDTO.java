package com.serverproject.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SignupOtpVerificationDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Email OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "Email OTP must be 6 digits")
    private String emailOtp;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmailOtp() { return emailOtp; }
    public void setEmailOtp(String emailOtp) { this.emailOtp = emailOtp; }
}
