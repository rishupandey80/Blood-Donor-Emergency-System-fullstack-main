package com.serverproject.service;

import com.serverproject.DTO.BloodRequestDTO;
import com.serverproject.DTO.EligibilityResponseDTO;
import com.serverproject.enums.NotificationType;
import com.serverproject.enums.RequestStatus;
import com.serverproject.enums.Role;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.mapper.BloodRequestMapper;
import com.serverproject.model.BloodBank;
import com.serverproject.model.BloodRequest;
import com.serverproject.model.BloodStock;
import com.serverproject.model.Donation;
import com.serverproject.model.Donor;
import com.serverproject.model.User;
import com.serverproject.repository.BloodBankRepository;
import com.serverproject.repository.BloodRequestRepository;
import com.serverproject.repository.BloodStockRepository;
import com.serverproject.repository.DonorRepository;
import com.serverproject.repository.UserRepository;
import com.serverproject.util.BloodCompatibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BloodRequestService {

    private static final Logger log = LoggerFactory.getLogger(BloodRequestService.class);

    @Autowired private BloodRequestRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private BloodBankRepository bloodBankRepository;
    @Autowired private BloodStockRepository bloodStockRepository;
    @Autowired private DonationService donationService;
    @Autowired private NotificationService notificationService;

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<BloodRequest> findById(Long id) {
        return repository.findById(id);
    }

    public boolean isVerifiedDonor(User user) {
        return donorRepository.existsByUserIdAndVerificationStatus(user.getId(), VerificationStatus.VERIFIED);
    }

    public boolean isVerifiedBloodBank(User user) {
        return bloodBankRepository.existsByUserIdAndVerificationStatus(user.getId(), VerificationStatus.VERIFIED);
    }

    public EligibilityResponseDTO donorEligibility(User user) {
        return donorRepository.findByUserId(user.getId())
                .map(donationService::checkEligibility)
                .orElse(null);
    }

    public boolean isBloodCompatible(User donorUser, BloodRequest request) {
        return donorRepository.findByUserId(donorUser.getId())
                .map(d -> BloodCompatibility.canDonate(d.getBloodGroup(), request.getBloodGroup()))
                .orElse(false);
    }

    /**
     * A bank can see/accept a request only when it is verified and has enough
     * units of a red-cell compatible blood group in its recorded inventory.
     */
    public boolean hasCompatibleStock(User bankUser, BloodRequest request) {
        if (bankUser == null || request == null || !isVerifiedBloodBank(bankUser)) {
            return false;
        }

        Optional<BloodBank> bank = bloodBankRepository.findByUserId(bankUser.getId());
        if (bank.isEmpty()) {
            return false;
        }

        return bloodStockRepository.findByBloodBankId(bank.get().getId()).stream()
                .filter(stock -> stock.getUnitsAvailable() >= request.getUnitsNeeded())
                .anyMatch(stock -> BloodCompatibility.canDonate(stock.getBloodGroup(), request.getBloodGroup()));
    }

    public BloodRequest createRequest(User requester, BloodRequestDTO dto) {
        if (requester.getRole() != Role.USER && requester.getRole() != Role.HOSPITAL) {
            throw new IllegalArgumentException("Only USER and HOSPITAL accounts can create blood requests.");
        }

        BloodRequest request = BloodRequestMapper.toEntity(dto);
        request.setRequestedBy(requester);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        BloodRequest saved = repository.save(request);

        // Notification failure must not discard a successfully saved emergency request.
        try {
            notificationService.notifyNewRequest(saved);
        } catch (Exception ex) {
            log.error("Request #{} was saved, but recipient notification creation failed: {}",
                    saved.getId(), ex.getMessage(), ex);
        }
        return saved;
    }

    public List<BloodRequest> getAllRequests() {
        return repository.findAll();
    }

    /** Incoming feed is only for pending requests. Accepted requests disappear
     * from everyone else's incoming feed and remain in the accepter's own feed. */
    public List<BloodRequest> getActiveRequests() {
        return repository.findByStatusIn(List.of(RequestStatus.PENDING));
    }

    public List<BloodRequest> getActiveRequestsFor(User user) {
        List<BloodRequest> pending = getActiveRequests();

        if (user.getRole() == Role.DONOR) {
            Optional<Donor> optionalDonor = donorRepository.findByUserId(user.getId());
            if (optionalDonor.isEmpty()) {
                return List.of();
            }

            Donor donor = optionalDonor.get();
            if (!donor.isAvailable() || !donationService.checkEligibility(donor).isEligible()) {
                return List.of();
            }

            return pending.stream()
                    .filter(request -> BloodCompatibility.canDonate(donor.getBloodGroup(), request.getBloodGroup()))
                    .collect(Collectors.toList());
        }

        if (user.getRole() == Role.BLOOD_BANK && isVerifiedBloodBank(user)) {
            return pending.stream()
                    .filter(request -> hasCompatibleStock(user, request))
                    .collect(Collectors.toList());
        }

        // Requesters use /mine and admins use /api/request.
        return List.of();
    }

    public List<BloodRequest> getMyRequestsFor(User user) {
        return repository.findByRequestedByIdOrderByCreatedAtDesc(user.getId());
    }

    public List<BloodRequest> getAcceptedRequestsFor(User user) {
        return repository.findByAcceptedByIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public BloodRequest accept(BloodRequest request, User accepter) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be accepted.");
        }
        if (accepter.getRole() == Role.DONOR) {
            EligibilityResponseDTO eligibility = donorEligibility(accepter);
            if (eligibility == null || !eligibility.isEligible() || !isBloodCompatible(accepter, request)) {
                throw new IllegalStateException("Donor profile, eligibility or blood compatibility check failed.");
            }
        } else if (accepter.getRole() == Role.BLOOD_BANK) {
            if (!hasCompatibleStock(accepter, request)) {
                throw new IllegalStateException("Verified blood bank does not have enough compatible stock for this request.");
            }
        } else {
            throw new IllegalArgumentException("Only donors and verified blood banks can accept requests.");
        }

        request.setAcceptedBy(accepter);
        request.setStatus(RequestStatus.ACCEPTED);
        BloodRequest saved = repository.save(request);

        notificationService.notify(saved.getRequestedBy(), NotificationType.REQUEST_ACCEPTED,
                "Good news! Your " + saved.getBloodGroup() + " blood request #" + saved.getId() + " was accepted.",
                saved.getId());
        notificationService.sendAcceptedAlerts(saved, accepter);
        return saved;
    }

    @Transactional
    public BloodRequest fulfill(BloodRequest request, User actor) {
        request.setStatus(RequestStatus.FULFILLED);
        BloodRequest saved = repository.save(request);
        Donation donation = donationService.recordDonation(saved);

        User acceptor = saved.getAcceptedBy();
        User requester = saved.getRequestedBy();

        if (acceptor != null && !sameUser(acceptor, actor)) {
            String message;
            if (donation != null) {
                LocalDate next = LocalDate.now().plusDays(DonationService.GAP_DAYS);
                message = "Thank you! Your donation for request #" + saved.getId()
                        + " was recorded. You can donate again from " + next + ".";
            } else {
                message = "Request #" + saved.getId() + " was marked as fulfilled. Thank you!";
            }
            notificationService.notify(acceptor, NotificationType.REQUEST_FULFILLED, message, saved.getId());
        }

        if (requester != null && !sameUser(requester, actor)) {
            notificationService.notify(requester, NotificationType.REQUEST_FULFILLED,
                    "Your " + saved.getBloodGroup() + " request #" + saved.getId() + " was marked as fulfilled.",
                    saved.getId());
        }
        return saved;
    }

    @Transactional
    public BloodRequest cancel(BloodRequest request, User actor) {
        request.setStatus(RequestStatus.CANCELED);
        BloodRequest saved = repository.save(request);

        String message = "Request #" + saved.getId() + " (" + saved.getBloodGroup()
                + ", " + saved.getLocation() + ") was canceled.";

        User acceptor = saved.getAcceptedBy();
        User requester = saved.getRequestedBy();

        if (acceptor != null && !sameUser(acceptor, actor)) {
            notificationService.notify(acceptor, NotificationType.REQUEST_CANCELED, message, saved.getId());
        }
        if (requester != null && !sameUser(requester, actor)) {
            notificationService.notify(requester, NotificationType.REQUEST_CANCELED, message, saved.getId());
        }
        return saved;
    }

    private boolean sameUser(User a, User b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }
}
