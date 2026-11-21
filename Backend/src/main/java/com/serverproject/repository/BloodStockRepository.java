package com.serverproject.repository;

import com.serverproject.enums.VerificationStatus;
import com.serverproject.model.BloodStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BloodStockRepository extends JpaRepository<BloodStock, Long> {

    List<BloodStock> findByBloodBankId(Long bloodBankId);

    Optional<BloodStock> findByBloodBankIdAndBloodGroup(Long bloodBankId, String bloodGroup);

    @Query("SELECT s FROM BloodStock s WHERE s.bloodGroup = :bloodGroup AND s.unitsAvailable > 0 " +
            "AND s.bloodBank.city = :city AND s.bloodBank.verificationStatus = :status")
    List<BloodStock> searchAvailable(@Param("bloodGroup") String bloodGroup,
                                     @Param("city") String city,
                                     @Param("status") VerificationStatus status);
}