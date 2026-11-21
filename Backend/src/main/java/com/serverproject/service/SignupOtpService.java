package com.serverproject.service;

import com.serverproject.DTO.SignupOtpVerificationDTO;
import com.serverproject.DTO.UserSignupDTO;
import com.serverproject.enums.Role;
import com.serverproject.mapper.UserMapper;
import com.serverproject.model.PendingSignupVerification;
import com.serverproject.model.User;
import com.serverproject.repository.PendingSignupVerificationRepository;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class SignupOtpService {

    private static final int MAX_ATTEMPTS = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private PendingSignupVerificationRepository pendingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.sms.default-country-code:+91}")
    private String defaultCountryCode;

    @Value("${app.signup-otp.expiry-seconds:60}")
    private long expirySeconds;

    @Value("${app.signup-otp.resend-seconds:60}")
    private long resendSeconds;

    public Map<String, Object> sendSignupOtps(UserSignupDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        String inputPhone = dto.getPhone().trim();
        String phone = toE164(inputPhone, defaultCountryCode);

        if (phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid phone number with country code or a 10-digit Indian mobile number.");
        }
        if (dto.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admin accounts cannot be created through public signup.");
        }
        if (!mailEnabled || mailSender == null || mailUsername == null || mailUsername.isBlank()
                || mailPassword == null || mailPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email OTP is not configured. Set MAIL_ENABLED=true, MAIL_USERNAME and MAIL_PASSWORD in Backend/.env.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        if (userRepository.existsByPhone(inputPhone) || userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already registered.");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<PendingSignupVerification> previousByEmail = pendingRepository.findByEmail(email);
        Optional<PendingSignupVerification> previousByPhone = pendingRepository.findByPhone(phone);

        if (previousByPhone.isPresent() && !previousByPhone.get().getEmail().equals(email)) {
            PendingSignupVerification phoneOwner = previousByPhone.get();
            if (phoneOwner.getExpiresAt() != null && now.isAfter(phoneOwner.getExpiresAt())) {
                pendingRepository.delete(phoneOwner);
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This phone number is already being used for another pending signup.");
            }
        }
        if (previousByEmail.isPresent()
                && previousByEmail.get().getLastSentAt().plusSeconds(resendSeconds).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait before requesting new OTPs.");
        }

        String emailOtp = generateOtp();

        PendingSignupVerification pending = previousByEmail.orElseGet(PendingSignupVerification::new);
        pending.setEmail(email);
        pending.setPhone(phone);
        pending.setName(dto.getName().trim());
        pending.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        pending.setGender(dto.getGender().trim());
        pending.setProfession(blankToNull(dto.getProfession()));
        pending.setLocation(blankToNull(dto.getLocation()));
        pending.setRole(dto.getRole());
        pending.setEmailOtpHash(passwordEncoder.encode(emailOtp));
        pending.setExpiresAt(now.plusSeconds(expirySeconds));
        pending.setLastSentAt(now);
        pending.setAttempts(0);
        pendingRepository.save(pending);

        try {
            sendEmailOtp(email, emailOtp);
        } catch (Exception ex) {
            pendingRepository.delete(pending);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email OTP delivery failed. Check Gmail SMTP settings in Backend/.env, then try again.", ex);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Email verification code has been sent. Verify your email to create your account.");
        response.put("expiresInSeconds", expirySeconds);
        return response;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Map<String, Object> verifyAndCreateAccount(SignupOtpVerificationDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        PendingSignupVerification pending = pendingRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No pending signup found. Request new OTPs first."));

        LocalDateTime now = LocalDateTime.now();
        if (pending.getExpiresAt() == null || !now.isBefore(pending.getExpiresAt())) {
            pendingRepository.delete(pending);
            throw new ResponseStatusException(HttpStatus.GONE, "OTP expired. Request new OTPs again.");
        }
        if (pending.getAttempts() >= MAX_ATTEMPTS) {
            pendingRepository.delete(pending);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect attempts. Request new OTPs.");
        }

        boolean emailOk = passwordEncoder.matches(dto.getEmailOtp(), pending.getEmailOtpHash());
        if (!emailOk) {
            pending.setAttempts(pending.getAttempts() + 1);
            pendingRepository.save(pending);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email OTP is incorrect. Please check the code and try again.");
        }

        if (userRepository.findByEmail(pending.getEmail()).isPresent()
                || userRepository.existsByPhone(pending.getPhone())) {
            pendingRepository.delete(pending);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email or phone number is already registered. Please sign in instead.");
        }

        User user = new User();
        user.setName(pending.getName());
        user.setEmail(pending.getEmail());
        user.setPhone(pending.getPhone());
        user.setPassword(pending.getPasswordHash());
        user.setGender(pending.getGender());
        user.setProfession(pending.getProfession());
        user.setLocation(pending.getLocation());
        user.setRole(pending.getRole());
        user.setVerified(true);
        user.setActive(true);
        user.setDeleted(false);

        User saved = userRepository.save(user);
        pendingRepository.delete(pending);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Email verified. Your account has been created successfully. Please sign in.");
        response.put("user", UserMapper.toResponseDTO(saved));
        return response;
    }

    private void sendEmailOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        String sender = (mailFrom == null || mailFrom.isBlank() || mailFrom.endsWith(".local"))
                ? mailUsername : mailFrom;
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("BloodBridge signup verification code");
        message.setText("Your BloodBridge email verification code is " + otp
                + ". It expires in " + expirySeconds + " seconds. Do not share this code with anyone.");
        mailSender.send(message);
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String toE164(String phone, String defaultCc) {
        if (phone == null) return null;
        String value = phone.replaceAll("[\\s\\-().]", "");
        if (value.startsWith("00")) value = "+" + value.substring(2);
        if (value.startsWith("+")) return value.matches("\\+\\d{8,15}") ? value : null;
        if (value.matches("\\d{10}")) return defaultCc + value;
        if (value.matches("0\\d{10}")) return defaultCc + value.substring(1);
        if (value.matches("\\d{11,15}")) return "+" + value;
        return null;
    }
}
