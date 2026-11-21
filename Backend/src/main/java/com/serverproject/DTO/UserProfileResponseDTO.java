package com.serverproject.DTO;

import com.serverproject.enums.Role;

public record UserProfileResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        String gender,
        String profession,
        String location,
        Role role,
        boolean active) {
}