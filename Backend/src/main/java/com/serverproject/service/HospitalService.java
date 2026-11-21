package com.serverproject.service;

import com.serverproject.DTO.HospitalRequestDTO;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.mapper.HospitalMapper;
import com.serverproject.model.Hospital;
import com.serverproject.model.User;
import com.serverproject.repository.HospitalRepository;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean hasProfile(Long userId) {
        return hospitalRepository.existsByUserId(userId);
    }

    public boolean registrationNumberTaken(String registrationNumber) {
        return hospitalRepository.existsByRegistrationNumber(registrationNumber);
    }

    public Hospital registerHospital(User user, HospitalRequestDTO dto) {
        Hospital hospital = HospitalMapper.toEntity(dto);
        hospital.setUser(user);
        hospital.setVerificationStatus(VerificationStatus.PENDING);
        return hospitalRepository.save(hospital);
    }

    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    public List<Hospital> searchByCity(String city) {
        return hospitalRepository.findByCity(city);
    }

    public Hospital updateVerificationStatus(Long hospitalId, VerificationStatus status) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElse(null);
        if (hospital != null) {
            hospital.setVerificationStatus(status);
            hospitalRepository.save(hospital);
            notificationService.notifyProfileStatus(hospital.getUser(), "hospital", status);
        }
        return hospital;
    }
}