package com.serverproject.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String otp,
        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters") String newPassword) {
}