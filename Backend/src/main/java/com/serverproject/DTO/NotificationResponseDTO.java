package com.serverproject.DTO;

import com.serverproject.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt,
        Long requestId) {
}