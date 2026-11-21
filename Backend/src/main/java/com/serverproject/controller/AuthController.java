package com.serverproject.controller;

import com.serverproject.DTO.LoginResponseDTO;
import com.serverproject.DTO.SignupOtpVerificationDTO;
import com.serverproject.DTO.UserSignupDTO;
import com.serverproject.enums.Role;
import com.serverproject.model.User;
import com.serverproject.repository.UserRepository;
import com.serverproject.security.JwtUtil;
import com.serverproject.service.SignupOtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SignupOtpService signupOtpService;

    @PostMapping("/signup/send-otp")
    public ResponseEntity<?> sendSignupOtps(@Valid @RequestBody UserSignupDTO signupDTO) {
        if (signupDTO.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest().body(Map.of("message", "Admin cannot be created via public signup."));
        }
        return ResponseEntity.ok(signupOtpService.sendSignupOtps(signupDTO));
    }

    @PostMapping("/signup/verify-otp")
    public ResponseEntity<?> verifySignupOtps(@Valid @RequestBody SignupOtpVerificationDTO dto) {
        return ResponseEntity.ok(signupOtpService.verifyAndCreateAccount(dto));
    }

    /** Direct account creation is intentionally disabled: email OTP verification is mandatory. */
    @PostMapping("/signup")
    public ResponseEntity<?> signupWithoutOtp() {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Signup requires email verification. Use /api/auth/signup/send-otp and /api/auth/signup/verify-otp."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Email and password are required.");
        }

        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();

            if (passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
                if (!existingUser.isActive() || existingUser.isDeleted()) {
                    return ResponseEntity.status(403).body("Your account is inactive. Please contact admin.");
                }

                String token = jwtUtil.generateToken(existingUser.getEmail(), existingUser.getRole());
                LoginResponseDTO response = new LoginResponseDTO(
                        token,
                        existingUser.getId(),
                        existingUser.getName(),
                        existingUser.getEmail(),
                        existingUser.getRole()
                );
                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.status(401).body("Invalid Credentials");
    }
}
