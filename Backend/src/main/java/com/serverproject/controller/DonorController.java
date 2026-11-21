package com.serverproject.controller;

import com.serverproject.DTO.DonorRequestDTO;
import com.serverproject.DTO.DonorResponseDTO;
import com.serverproject.enums.Role;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.mapper.DonorMapper;
import com.serverproject.model.Donor;
import com.serverproject.model.User;
import com.serverproject.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donors")
public class DonorController {

    @Autowired
    private DonorService donorService;

    @PostMapping("/add")
    public ResponseEntity<?> addDonor(Authentication auth, @Valid @RequestBody DonorRequestDTO dto) {

        Optional<User> user = donorService.findUserByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        if (user.get().getRole() != Role.DONOR) {
            return ResponseEntity.status(403).body("Only DONOR accounts can create a donor profile!");
        }

        if (donorService.hasProfile(user.get().getId())) {
            return ResponseEntity.status(409).body("Donor profile already exists for this account!");
        }

        Donor saved = donorService.addDonor(user.get(), dto);
        return ResponseEntity.ok(DonorMapper.toResponseDTO(saved));
    }

    @PutMapping("/update-availability")
    public ResponseEntity<?> updateAvailability(Authentication auth,
                                                @RequestParam Long donorId,
                                                @RequestParam boolean available) {

        Optional<User> user = donorService.findUserByEmail(auth.getName());
        Optional<Donor> optionalDonor = donorService.findById(donorId);

        if (user.isEmpty() || optionalDonor.isEmpty()) {
            return ResponseEntity.status(404).body("Donor or user not found!");
        }

        Donor donor = optionalDonor.get();

        boolean isOwner = donor.getUser() != null
                && donor.getUser().getId().equals(user.get().getId());

        if (!isOwner && user.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("You can only update your own availability!");
        }

        return ResponseEntity.ok(DonorMapper.toResponseDTO(donorService.setAvailability(donor, available)));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<?> verifyDonor(@PathVariable Long id) {
        Donor updated = donorService.updateVerificationStatus(id, VerificationStatus.VERIFIED);
        if (updated == null) {
            return ResponseEntity.badRequest().body("Donor not found!");
        }
        return ResponseEntity.ok(DonorMapper.toResponseDTO(updated));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectDonor(@PathVariable Long id) {
        Donor updated = donorService.updateVerificationStatus(id, VerificationStatus.REJECTED);
        if (updated == null) {
            return ResponseEntity.badRequest().body("Donor not found!");
        }
        return ResponseEntity.ok(DonorMapper.toResponseDTO(updated));
    }

    @GetMapping("/search")
    public List<DonorResponseDTO> searchDonors(@RequestParam String bloodGroup, @RequestParam String city) {
        return donorService.searchDonors(bloodGroup, city)
                .stream().map(DonorMapper::toResponseDTO).collect(Collectors.toList());
    }

    @GetMapping("/search/bloodgroup")
    public List<DonorResponseDTO> searchDonorsByBloodGroup(@RequestParam String bloodGroup) {
        return donorService.searchDonors(bloodGroup)
                .stream().map(DonorMapper::toResponseDTO).collect(Collectors.toList());
    }

    @GetMapping("/search/city")
    public List<DonorResponseDTO> searchDonorsByCity(@RequestParam String city) {
        return donorService.searchByCity(city)
                .stream().map(DonorMapper::toResponseDTO).collect(Collectors.toList());
    }

    @GetMapping
    public List<DonorResponseDTO> getAllDonors() {
        return donorService.getAllDonors()
                .stream().map(DonorMapper::toResponseDTO).collect(Collectors.toList());
    }
}