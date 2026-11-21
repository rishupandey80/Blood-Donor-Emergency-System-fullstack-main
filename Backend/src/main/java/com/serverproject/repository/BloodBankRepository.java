package com.serverproject.repository;

import com.serverproject.enums.VerificationStatus;
import com.serverproject.model.BloodBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BloodBankRepository extends JpaRepository<BloodBank, Long> {

    List<BloodBank> findByCity(String city);

    Optional<BloodBank> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByUserIdAndVerificationStatus(Long userId, VerificationStatus status);
}