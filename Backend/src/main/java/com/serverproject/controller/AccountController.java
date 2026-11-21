package com.serverproject.controller;

import com.serverproject.DTO.ChangePasswordRequestDTO;
import com.serverproject.DTO.UpdateProfileRequestDTO;
import com.serverproject.DTO.UserProfileResponseDTO;
import com.serverproject.enums.Role;
import com.serverproject.model.User;
import com.serverproject.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users/me")
public class AccountController {

    @Autowired
    private AccountService service;

    @GetMapping
    public ResponseEntity<?> me(Authentication auth) {
        Optional<User> user = service.findByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }
        return ResponseEntity.ok(toProfile(user.get()));
    }

    @PutMapping
    public ResponseEntity<?> update(Authentication auth, @Valid @RequestBody UpdateProfileRequestDTO dto) {
        Optional<User> user = service.findByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        if (service.phoneTaken(dto.phone().trim(), user.get().getId())) {
            return ResponseEntity.status(409).body("Phone number already in use!");
        }

        return ResponseEntity.ok(toProfile(service.updateProfile(user.get(), dto)));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequestDTO dto) {
        Optional<User> user = service.findByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        if (!service.passwordMatches(user.get(), dto.oldPassword())) {
            return ResponseEntity.badRequest().body("Old password is incorrect!");
        }

        if (dto.oldPassword().equals(dto.newPassword())) {
            return ResponseEntity.badRequest().body("New password must be different from the old one!");
        }

        service.changePassword(user.get(), dto.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(Authentication auth) {
        Optional<User> user = service.findByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        if (user.get().getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).body("Admin accounts cannot be deleted here!");
        }

        service.deleteAccount(user.get());
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }

    private UserProfileResponseDTO toProfile(User u) {
        return new UserProfileResponseDTO(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getGender(), u.getProfession(), u.getLocation(), u.getRole(), u.isActive());
    }
}