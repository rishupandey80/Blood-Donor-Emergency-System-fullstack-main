
package com.serverproject.controller;

import com.serverproject.DTO.ForgotPasswordRequestDTO;
import com.serverproject.DTO.ResetPasswordRequestDTO;
import com.serverproject.service.PasswordResetService;
import com.serverproject.service.MessageDispatcher;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private static final Logger log =
            LoggerFactory.getLogger(PasswordResetController.class);

    @Value("${app.dev.expose-otp:false}")
    private boolean exposeOtp;

    @Autowired
    private PasswordResetService service;

    @Autowired
    private MessageDispatcher messageDispatcher;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO dto) {

        Optional<String> otp = service.createOtp(dto.email());

        Map<String, Object> body = new LinkedHashMap<>();

        body.put(
                "message",
                "If this email is registered, an OTP has been sent."
        );

        if (otp.isPresent()) {

            String email = dto.email().trim();

            String subject = "BloodBridge - Password Reset OTP";

            String message =
                    "Hello,\n\n" +
                            "Your BloodBridge password reset OTP is: " + otp.get() +
                            "\n\nThis OTP is valid for 10 minutes." +
                            "\n\nPlease do not share this OTP with anyone." +
                            "\n\nRegards,\nBloodBridge Team";

            // Send OTP to registered email
            messageDispatcher.sendEmail(email, subject, message);

            log.info("Password reset email requested for {}", email);

            // Development only
            if (exposeOtp) {
                body.put("devOtp", otp.get());
            }
        }

        return ResponseEntity.ok(body);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto) {

        boolean ok = service.resetPassword(
                dto.email(),
                dto.otp(),
                dto.newPassword()
        );

        if (!ok) {
            return ResponseEntity.badRequest()
                    .body("Invalid or expired OTP!");
        }

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password reset successful. Please login."
                )
        );
    }
}
