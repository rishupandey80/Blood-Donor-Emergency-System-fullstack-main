package com.serverproject.controller;

import com.serverproject.DTO.BloodBankRequestDTO;
import com.serverproject.DTO.BloodBankResponseDTO;
import com.serverproject.DTO.BloodStockRequestDTO;
import com.serverproject.DTO.BloodStockResponseDTO;
import com.serverproject.enums.Role;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.mapper.BloodBankMapper;
import com.serverproject.model.BloodBank;
import com.serverproject.model.BloodStock;
import com.serverproject.model.User;
import com.serverproject.service.BloodBankService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bloodbanks")
public class BloodBankController {

    @Autowired
    private BloodBankService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(Authentication auth, @Valid @RequestBody BloodBankRequestDTO dto) {

        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        if (user.get().getRole() != Role.BLOOD_BANK) {
            return ResponseEntity.status(403).body("Only BLOOD_BANK accounts can register a blood bank profile!");
        }

        if (service.hasProfile(user.get().getId())) {
            return ResponseEntity.status(409).body("Blood bank profile already exists for this account!");
        }

        if (service.licenseTaken(dto.getLicenseNumber())) {
            return ResponseEntity.status(409).body("This license number is already registered!");
        }

        BloodBank saved = service.register(user.get(), dto);
        return ResponseEntity.ok(BloodBankMapper.toResponseDTO(saved));
    }

    @GetMapping
    public List<BloodBankResponseDTO> getAll() {
        return service.getAll()
                .stream().map(BloodBankMapper::toResponseDTO).collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<BloodBankResponseDTO> searchByCity(@RequestParam String city) {
        return service.searchByCity(city)
                .stream().map(BloodBankMapper::toResponseDTO).collect(Collectors.toList());
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<?> verify(@PathVariable Long id) {
        BloodBank updated = service.updateVerificationStatus(id, VerificationStatus.VERIFIED);
        if (updated == null) {
            return ResponseEntity.badRequest().body("Blood bank not found!");
        }
        return ResponseEntity.ok(BloodBankMapper.toResponseDTO(updated));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        BloodBank updated = service.updateVerificationStatus(id, VerificationStatus.REJECTED);
        if (updated == null) {
            return ResponseEntity.badRequest().body("Blood bank not found!");
        }
        return ResponseEntity.ok(BloodBankMapper.toResponseDTO(updated));
    }

    @PutMapping("/stock")
    public ResponseEntity<?> updateStock(Authentication auth, @Valid @RequestBody BloodStockRequestDTO dto) {

        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        Optional<BloodBank> bank = service.findByUserId(user.get().getId());
        if (bank.isEmpty()) {
            return ResponseEntity.badRequest().body("Register your blood bank profile first!");
        }

        BloodStock saved = service.setStock(bank.get(), dto.getBloodGroup(), dto.getUnitsAvailable());
        return ResponseEntity.ok(BloodBankMapper.toStockResponseDTO(saved));
    }

    @GetMapping("/stock/mine")
    public ResponseEntity<?> myStock(Authentication auth) {

        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        Optional<BloodBank> bank = service.findByUserId(user.get().getId());
        if (bank.isEmpty()) {
            return ResponseEntity.badRequest().body("Register your blood bank profile first!");
        }

        List<BloodStockResponseDTO> stock = service.getStock(bank.get().getId())
                .stream().map(BloodBankMapper::toStockResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/stock/search")
    public List<BloodStockResponseDTO> searchStock(@RequestParam String bloodGroup, @RequestParam String city) {
        return service.searchStock(bloodGroup.trim().toUpperCase(), city)
                .stream().map(BloodBankMapper::toStockResponseDTO).collect(Collectors.toList());
    }
}