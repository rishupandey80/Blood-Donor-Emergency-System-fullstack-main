package com.serverproject.service;

import com.serverproject.model.BloodBank;
import com.serverproject.model.BloodRequest;
import com.serverproject.model.Donor;
import com.serverproject.model.Hospital;
import com.serverproject.model.User;
import com.serverproject.repository.BloodBankRepository;
import com.serverproject.repository.BloodRequestRepository;
import com.serverproject.repository.DonorRepository;
import com.serverproject.repository.HospitalRepository;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    public Optional<User> findUser(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<Donor> myDonor(Long userId) {
        return donorRepository.findByUserId(userId);
    }

    public Optional<Hospital> myHospital(Long userId) {
        return hospitalRepository.findByUserId(userId);
    }

    public Optional<BloodBank> myBloodBank(Long userId) {
        return bloodBankRepository.findByUserId(userId);
    }

    public List<BloodRequest> requestsCreatedBy(Long userId) {
        return bloodRequestRepository.findByRequestedByIdOrderByCreatedAtDesc(userId);
    }

    public List<BloodRequest> requestsAcceptedBy(Long userId) {
        return bloodRequestRepository.findByAcceptedByIdOrderByCreatedAtDesc(userId);
    }
}