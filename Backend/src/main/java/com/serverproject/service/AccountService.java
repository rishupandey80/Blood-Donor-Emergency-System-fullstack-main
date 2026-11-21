package com.serverproject.service;

import com.serverproject.DTO.UpdateProfileRequestDTO;
import com.serverproject.enums.RequestStatus;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.model.BloodRequest;
import com.serverproject.model.User;
import com.serverproject.repository.BloodBankRepository;
import com.serverproject.repository.BloodRequestRepository;
import com.serverproject.repository.BloodStockRepository;
import com.serverproject.repository.DonorRepository;
import com.serverproject.repository.HospitalRepository;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodStockRepository stockRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean phoneTaken(String phone, Long userId) {
        return userRepository.existsByPhoneAndIdNot(phone, userId);
    }

    public User updateProfile(User user, UpdateProfileRequestDTO dto) {
        user.setName(dto.name().trim());
        user.setPhone(dto.phone().trim());
        user.setGender(dto.gender().trim());
        user.setProfession(dto.profession());
        user.setLocation(dto.location());
        return userRepository.save(user);
    }

    public boolean passwordMatches(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(User user) {
        Long id = user.getId();

        // 1. Is user ki banayi hui open requests cancel
        for (BloodRequest r : bloodRequestRepository.findByRequestedByIdOrderByCreatedAtDesc(id)) {
            if (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.ACCEPTED) {
                r.setStatus(RequestStatus.CANCELED);
                bloodRequestRepository.save(r);
            }
        }

        // 2. Jo requests is user ne accept ki thi par pura nahi ki, wo wapas PENDING
        for (BloodRequest r : bloodRequestRepository.findByAcceptedByIdOrderByCreatedAtDesc(id)) {
            if (r.getStatus() == RequestStatus.ACCEPTED) {
                r.setAcceptedBy(null);
                r.setStatus(RequestStatus.PENDING);
                bloodRequestRepository.save(r);
            }
        }

        // 3. Profiles
        donorRepository.findByUserId(id).ifPresent(d -> {
            d.setName("Deleted Donor");
            d.setPhone(null);
            d.setEmail(null);
            d.setAddress(null);
            d.setAvailable(false);
            d.setVerificationStatus(VerificationStatus.REJECTED);
            donorRepository.save(d);
        });

        hospitalRepository.findByUserId(id).ifPresent(hospitalRepository::delete);

        bloodBankRepository.findByUserId(id).ifPresent(bank -> {
            stockRepository.deleteAll(stockRepository.findByBloodBankId(bank.getId()));
            bloodBankRepository.delete(bank);
        });

        // 4. User ka personal data hatao, account band
        user.setName("Deleted User");
        user.setEmail("deleted_" + id + "@deleted.invalid");
        user.setPhone("deleted-" + id);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setProfession(null);
        user.setLocation(null);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
        user.setActive(false);
        user.setDeleted(true);
        userRepository.save(user);
    }
}