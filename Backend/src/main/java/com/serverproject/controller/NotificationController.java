package com.serverproject.controller;

import com.serverproject.DTO.NotificationResponseDTO;
import com.serverproject.model.Notification;
import com.serverproject.model.User;
import com.serverproject.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @GetMapping
    public ResponseEntity<?> list(Authentication auth) {
        Optional<User> user = service.findUser(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        List<NotificationResponseDTO> list = service.latest(user.get().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(Authentication auth) {
        Optional<User> user = service.findUser(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }
        return ResponseEntity.ok(Map.of("count", service.unreadCount(user.get().getId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        Optional<User> user = service.findUser(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        if (!service.markRead(id, user.get().getId())) {
            return ResponseEntity.status(404).body("Notification not found!");
        }
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        Optional<User> user = service.findUser(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }
        return ResponseEntity.ok(Map.of("updated", service.markAllRead(user.get().getId())));
    }

    private NotificationResponseDTO toDto(Notification n) {
        return new NotificationResponseDTO(n.getId(), n.getType(), n.getMessage(),
                n.isReadFlag(), n.getCreatedAt(), n.getRequestId());
    }
}