package com.serverproject.controller;

import com.serverproject.DTO.HospitalRequestDTO;
import com.serverproject.DTO.HospitalResponseDTO;
import com.serverproject.enums.Role;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.mapper.HospitalMapper;
import com.serverproject.model.Hospital;
import com.serverproject.model.User;
import com.serverproject.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @PostMapping("/register")
    public ResponseEntity<?> registerHospital(Authentication auth, @Valid @RequestBody HospitalRequestDTO dto) {

        Optional<User> user = hospitalService.findUserByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        if (user.get().getRole() != Role.HOSPITAL) {
            return ResponseEntity.status(403).body("Only HOSPITAL accounts can register a hospital profile!");
        }

        if (hospitalService.hasProfile(user.get().getId())) {
            return ResponseEntity.status(409).body("Hospital profile already exists for this account!");
        }

        if (hospitalService.registrationNumberTaken(dto.getRegistrationNumber())) {
            return ResponseEntity.status(409).body("This registration number is already registered!");
        }

        Hospital saved = hospitalService.registerHospital(user.get(), dto);
        return ResponseEntity.ok(HospitalMapper.toResponseDTO(saved));
    }

    @GetMapping
    public List<HospitalResponseDTO> getAllHospitals() {
        return hospitalService.getAllHospitals()
                .stream().map(HospitalMapper::toResponseDTO).collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<HospitalResponseDTO> searchByCity(@RequestParam String city) {
        return hospitalService.searchByCity(city)
                .stream().map(HospitalMapper::toResponseDTO).collect(Collectors.toList());
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<?> verifyHospital(@PathVariable Long id) {
        Hospital updated = hospitalService.updateVerificationStatus(id, VerificationStatus.VERIFIED);
        if (updated == null) {
            return ResponseEntity.badRequest().body("Hospital not found!");
        }
        return ResponseEntity.ok(HospitalMapper.toResponseDTO(updated));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectHospital(@PathVariable Long id) {
        Hospital updated = hospitalService.updateVerificationStatus(id, VerificationStatus.REJECTED);
        if (updated == null) {
            return ResponseEntity.badRequest().body("Hospital not found!");
        }
        return ResponseEntity.ok(HospitalMapper.toResponseDTO(updated));
    }
}