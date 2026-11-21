package com.serverproject.service;

import com.serverproject.DTO.EligibilityResponseDTO;
import com.serverproject.enums.Role;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.model.BloodRequest;
import com.serverproject.model.Donation;
import com.serverproject.model.Donor;
import com.serverproject.model.User;
import com.serverproject.repository.DonationRepository;
import com.serverproject.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DonationService {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 65;
    public static final double MIN_WEIGHT = 50.0;
    public static final int GAP_DAYS = 90;
    public static final int UNITS_PER_DONATION = 1;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private DonationRepository donationRepository;

    public Optional<Donor> findDonorByEmail(String email) {
        return donorRepository.findByUserEmail(email);
    }

    public List<Donation> getHistory(Long donorId) {
        return donationRepository.findByDonorIdOrderByDonationDateDesc(donorId);
    }

    public EligibilityResponseDTO checkEligibility(Donor donor) {
        List<String> reasons = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if (donor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            reasons.add("Donor profile is not verified yet (status: " + donor.getVerificationStatus() + ")");
        }

        if (donor.getAge() < MIN_AGE || donor.getAge() > MAX_AGE) {
            reasons.add("Age must be between " + MIN_AGE + " and " + MAX_AGE);
        }

        if (donor.getWeight() < MIN_WEIGHT) {
            reasons.add("Weight must be at least " + MIN_WEIGHT + " kg");
        }

        LocalDate last = donor.getLastDonationDate();
        LocalDate next = null;
        if (last != null) {
            next = last.plusDays(GAP_DAYS);
            if (today.isBefore(next)) {
                reasons.add("Last donation was on " + last + ". You can donate again from " + next);
            }
        }

        EligibilityResponseDTO dto = new EligibilityResponseDTO();
        dto.setEligible(reasons.isEmpty());
        dto.setReasons(reasons);
        dto.setLastDonationDate(last);
        dto.setNextEligibleDate(next);
        return dto;
    }

    @Transactional
    public Donation recordDonation(BloodRequest request) {
        User acceptor = request.getAcceptedBy();
        if (acceptor == null || acceptor.getRole() != Role.DONOR) {
            return null;
        }

        Optional<Donor> optionalDonor = donorRepository.findByUserId(acceptor.getId());
        if (optionalDonor.isEmpty()) {
            return null;
        }

        if (donationRepository.existsByBloodRequestId(request.getId())) {
            return null;
        }

        Donor donor = optionalDonor.get();
        LocalDate today = LocalDate.now();

        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setBloodRequest(request);
        donation.setDonationDate(today);
        donation.setUnitsDonated(UNITS_PER_DONATION);
        Donation saved = donationRepository.save(donation);

        donor.setLastDonationDate(today);
        donorRepository.save(donor);

        return saved;
    }
}