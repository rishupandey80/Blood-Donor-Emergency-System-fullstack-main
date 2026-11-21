package com.serverproject.controller;

import com.serverproject.mapper.BloodBankMapper;
import com.serverproject.mapper.DonorMapper;
import com.serverproject.mapper.HospitalMapper;
import com.serverproject.model.BloodBank;
import com.serverproject.model.Donor;
import com.serverproject.model.Hospital;
import com.serverproject.model.User;
import com.serverproject.service.MeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class MeController {

    @Autowired
    private MeService service;

    @GetMapping("/api/donors/me")
    public ResponseEntity<?> myDonorProfile(Authentication auth) {
        Optional<User> user = service.findUser(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        Optional<Donor> donor = service.myDonor(user.get().getId());
        if (donor.isEmpty()) {
            return ResponseEntity.status(404).body("Donor profile not found. Please create one first.");
        }
        return ResponseEntity.ok(DonorMapper.toResponseDTO(donor.get()));
    }

    @GetMapping("/api/hospitals/me")
    public ResponseEntity<?> myHospitalProfile(Authentication auth) {
        Optional<User> user = service.findUser(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        Optional<Hospital> hospital = service.myHospital(user.get().getId());
        if (hospital.isEmpty()) {
            return ResponseEntity.status(404).body("Hospital profile not found. Please register first.");
        }
        return ResponseEntity.ok(HospitalMapper.toResponseDTO(hospital.get()));
    }

    @GetMapping("/api/bloodbanks/me")
    public ResponseEntity<?> myBloodBankProfile(Authentication auth) {
        Optional<User> user = service.findUser(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }

        Optional<BloodBank> bank = service.myBloodBank(user.get().getId());
        if (bank.isEmpty()) {
            return ResponseEntity.status(404).body("Blood bank profile not found. Please register first.");
        }
        return ResponseEntity.ok(BloodBankMapper.toResponseDTO(bank.get()));
    }

    // Request listing endpoints (/api/request/mine and /api/request/accepted-by-me)
    // are handled by BloodRequestController. Do not duplicate those mappings here.
}
