package com.serverproject.repository;

import com.serverproject.model.PendingSignupVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingSignupVerificationRepository
        extends JpaRepository<PendingSignupVerification, Long> {

    Optional<PendingSignupVerification> findByEmail(String email);

    Optional<PendingSignupVerification> findByPhone(String phone);
}
