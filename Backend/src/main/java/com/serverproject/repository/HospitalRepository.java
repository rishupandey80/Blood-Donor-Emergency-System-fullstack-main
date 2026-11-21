package com.serverproject.repository;

import com.serverproject.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    List<Hospital> findByCity(String city);

    List<Hospital> findByRegistrationNumber(String registrationNumber);

    boolean existsByUserId(Long userId);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Hospital> findByUserId(Long userId);
}