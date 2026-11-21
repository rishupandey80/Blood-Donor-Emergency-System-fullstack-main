package com.serverproject.controller;

import com.serverproject.DTO.UserResponseDTO;
import com.serverproject.enums.Role;
import com.serverproject.mapper.UserMapper;
import com.serverproject.model.User;
import com.serverproject.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers(@RequestParam(required = false) Role role) {
        return adminService.getAllUsers()
                .stream()
                .filter(u -> role == null || u.getRole() == role)
                .map(UserMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @PutMapping("/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id) {
        return changeStatus(id, false);
    }

    @PutMapping("/users/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long id) {
        return changeStatus(id, true);
    }

    private ResponseEntity<?> changeStatus(Long id, boolean active) {
        Optional<User> optionalUser = adminService.findUser(id);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(404).body("User not found!");
        }

        User user = optionalUser.get();

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest().body("Admin accounts cannot be banned or unbanned!");
        }

        User updated = adminService.updateActiveStatus(user, active);
        return ResponseEntity.ok(UserMapper.toResponseDTO(updated));
    }
}