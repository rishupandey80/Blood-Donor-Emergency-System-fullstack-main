package com.serverproject.repository;

import com.serverproject.enums.VerificationStatus;
import com.serverproject.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {

    List<Donor> findByBloodGroupAndCity(String bloodGroup, String city);

    List<Donor> findByBloodGroup(String bloodGroup);

    List<Donor> findByCity(String city);

    List<Donor> findByAvailable(boolean available);

    List<Donor> findByBloodGroupAndCityAndAvailable(String bloodGroup, String city, boolean available);

    List<Donor> findByBloodGroupAndCityAndAvailableAndVerificationStatus(
            String bloodGroup, String city, boolean available, VerificationStatus status);

    List<Donor> findByBloodGroupAndAvailableAndVerificationStatus(
            String bloodGroup, boolean available, VerificationStatus status);

    List<Donor> findByCityAndAvailableAndVerificationStatus(
            String city, boolean available, VerificationStatus status);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndVerificationStatus(Long userId, VerificationStatus status);

    Optional<Donor> findByUserId(Long userId);

    Optional<Donor> findByUserEmail(String email);
}