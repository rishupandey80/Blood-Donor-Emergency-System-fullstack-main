package com.serverproject.service;

import com.serverproject.DTO.DonorRequestDTO;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.mapper.DonorMapper;
import com.serverproject.model.Donor;
import com.serverproject.model.User;
import com.serverproject.repository.DonorRepository;
import com.serverproject.repository.UserRepository;
import com.serverproject.util.BloodCompatibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DonorService {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean hasProfile(Long userId) {
        return donorRepository.existsByUserId(userId);
    }

    public Optional<Donor> findById(Long id) {
        return donorRepository.findById(id);
    }

    public Donor addDonor(User user, DonorRequestDTO dto) {
        Donor donor = DonorMapper.toEntity(dto);
        donor.setUser(user);
        donor.setAvailable(true);
        donor.setVerificationStatus(VerificationStatus.PENDING);
        return donorRepository.save(donor);
    }

    public Donor setAvailability(Donor donor, boolean available) {
        donor.setAvailable(available);
        return donorRepository.save(donor);
    }

    public Donor updateVerificationStatus(Long donorId, VerificationStatus status) {
        Donor donor = donorRepository.findById(donorId).orElse(null);
        if (donor != null) {
            donor.setVerificationStatus(status);
            donorRepository.save(donor);
            notificationService.notifyProfileStatus(donor.getUser(), "donor", status);
        }
        return donor;
    }

    public List<Donor> searchDonors(String bloodGroup, String city) {
        return donorRepository.findByBloodGroupAndCityAndAvailableAndVerificationStatus(
                BloodCompatibility.normalize(bloodGroup), city, true, VerificationStatus.VERIFIED);
    }

    public List<Donor> searchDonors(String bloodGroup) {
        return donorRepository.findByBloodGroupAndAvailableAndVerificationStatus(
                BloodCompatibility.normalize(bloodGroup), true, VerificationStatus.VERIFIED);
    }

    public List<Donor> searchByCity(String city) {
        return donorRepository.findByCityAndAvailableAndVerificationStatus(
                city, true, VerificationStatus.VERIFIED);
    }

    public List<Donor> getAllDonors() {
        return donorRepository.findAll();
    }
}