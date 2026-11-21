package com.serverproject.DTO;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequestDTO(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Phone is required") String phone,
        @NotBlank(message = "Gender is required") String gender,
        String profession,
        String location) {
}