package com.serverproject.service;

import com.serverproject.model.User;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final int OTP_VALID_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // OTP bana ke hash store karta hai. Email registered nahi ho to Optional.empty()
    public Optional<String> createOtp(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email.trim());
        if (optionalUser.isEmpty() || optionalUser.get().isDeleted()) {
            return Optional.empty();
        }

        User user = optionalUser.get();
        String otp = String.format("%06d", random.nextInt(1_000_000));

        user.setOtp(passwordEncoder.encode(otp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
        user.setOtpAttempts(0);
        userRepository.save(user);

        return Optional.of(otp);
    }

    public boolean resetPassword(String email, String otp, String newPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(email.trim());
        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();

        if (user.isDeleted()
                || user.getOtp() == null
                || user.getOtpExpiry() == null
                || LocalDateTime.now().isAfter(user.getOtpExpiry())
                || user.getOtpAttempts() >= MAX_ATTEMPTS) {
            return false;
        }

        if (!passwordEncoder.matches(otp, user.getOtp())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            if (user.getOtpAttempts() >= MAX_ATTEMPTS) {
                user.setOtp(null);
                user.setOtpExpiry(null);
            }
            userRepository.save(user);
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
        userRepository.save(user);
        return true;
    }
}