package com.serverproject.service;

import com.serverproject.enums.NotificationType;
import com.serverproject.enums.Role;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.model.BloodBank;
import com.serverproject.model.BloodRequest;
import com.serverproject.model.BloodStock;
import com.serverproject.model.Donor;
import com.serverproject.model.Notification;
import com.serverproject.model.User;
import com.serverproject.repository.BloodBankRepository;
import com.serverproject.repository.BloodStockRepository;
import com.serverproject.repository.DonorRepository;
import com.serverproject.repository.NotificationRepository;
import com.serverproject.repository.UserRepository;
import com.serverproject.util.BloodCompatibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired private NotificationRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private BloodBankRepository bloodBankRepository;
    @Autowired private BloodStockRepository bloodStockRepository;
    @Autowired private DonationService donationService;
    @Autowired private MessageDispatcher dispatcher;

    public Optional<User> findUser(String email) {
        return userRepository.findByEmail(email);
    }

    public void notify(User user, NotificationType type, String message, Long requestId) {
        if (user == null) return;
        repository.save(build(user, type, message, requestId));
    }

    /**
     * Creates in-app notifications only for recipients who can actually respond:
     * donors must be verified, available, eligible and blood-compatible; blood
     * banks must be verified and hold enough compatible stock.
     */
    @Transactional
    public void notifyNewRequest(BloodRequest request) {
        Long requesterId = request.getRequestedBy() != null ? request.getRequestedBy().getId() : null;
        String message = "New " + request.getBloodGroup() + " blood request for " + request.getUnitsNeeded()
                + " unit(s) in " + request.getLocation() + " (urgency: " + request.getUrgency()
                + "). Open Incoming Requests to respond.";

        List<Notification> batch = new ArrayList<>();
        int donorRecipients = 0;
        int bankRecipients = 0;

        for (Donor donor : donorRepository.findByAvailable(true)) {
            User recipient = donor.getUser();
            if (donor.getVerificationStatus() != VerificationStatus.VERIFIED
                    || !usable(recipient, requesterId)
                    || recipient.getRole() != Role.DONOR
                    || !BloodCompatibility.canDonate(donor.getBloodGroup(), request.getBloodGroup())
                    || !donationService.checkEligibility(donor).isEligible()) {
                continue;
            }

            batch.add(build(recipient, NotificationType.NEW_REQUEST, message, request.getId()));
            donorRecipients++;
        }

        for (BloodBank bank : bloodBankRepository.findAll()) {
            User recipient = bank.getUser();
            if (bank.getVerificationStatus() != VerificationStatus.VERIFIED
                    || !usable(recipient, requesterId)
                    || recipient.getRole() != Role.BLOOD_BANK
                    || !hasCompatibleStock(bank, request)) {
                continue;
            }

            batch.add(build(recipient, NotificationType.NEW_REQUEST, message, request.getId()));
            bankRecipients++;
        }

        if (!batch.isEmpty()) {
            repository.saveAll(batch);
        }

        log.info("Request #{} notification routing complete: {} eligible donor(s), {} matching verified blood bank(s), {} notification(s) saved.",
                request.getId(), donorRecipients, bankRecipients, batch.size());
    }

    private boolean hasCompatibleStock(BloodBank bank, BloodRequest request) {
        if (bank == null || request == null) return false;
        return bloodStockRepository.findByBloodBankId(bank.getId()).stream()
                .filter(stock -> stock.getUnitsAvailable() >= request.getUnitsNeeded())
                .anyMatch(stock -> BloodCompatibility.canDonate(stock.getBloodGroup(), request.getBloodGroup()));
    }

    // Request accept hone par requester ko email + SMS (background mein, fail hone par accept nahi rukta).
    public void sendAcceptedAlerts(BloodRequest request, User accepter) {
        User requester = request.getRequestedBy();
        if (requester == null || accepter == null || requester.isDeleted() || !requester.isActive()) {
            return;
        }

        String who = clean(accepter.getName(), 40);
        String role = accepter.getRole() == Role.BLOOD_BANK ? "Blood Bank" : "Donor";
        String phone = accepter.getPhone();

        String sms = "BloodConnect: your " + request.getBloodGroup() + " request #" + request.getId()
                + " was accepted by " + who + " (" + role + "). Contact: " + phone;
        dispatcher.sendSms(requester.getPhone(), sms);

        String body = "Hello " + clean(requester.getName(), 60) + ",\n\n"
                + "Good news! Your blood request #" + request.getId() + " has been accepted.\n\n"
                + "Request : " + request.getBloodGroup() + ", " + request.getUnitsNeeded()
                + " unit(s), " + clean(request.getLocation(), 80) + "\n"
                + "Patient : " + clean(request.getPatientName(), 60) + "\n\n"
                + "Accepted by : " + who + " (" + role + ")\n"
                + "Phone       : " + phone + "\n\n"
                + "Please contact them directly to arrange the donation. "
                + "Once the blood is received, mark the request as fulfilled in the app.\n\n"
                + "- BloodConnect";
        dispatcher.sendEmail(requester.getEmail(),
                "Your blood request #" + request.getId() + " has been accepted", body);
    }

    public void notifyProfileStatus(User user, String profileLabel, VerificationStatus status) {
        if (status == VerificationStatus.VERIFIED) {
            notify(user, NotificationType.PROFILE_VERIFIED,
                    "Your " + profileLabel + " profile has been verified.", null);
        } else if (status == VerificationStatus.REJECTED) {
            notify(user, NotificationType.PROFILE_REJECTED,
                    "Your " + profileLabel + " profile was rejected. Please contact support.", null);
        }
    }

    public List<Notification> latest(Long userId) {
        return repository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFlagFalse(userId);
    }

    public boolean markRead(Long notificationId, Long userId) {
        Optional<Notification> found = repository.findById(notificationId);
        if (found.isEmpty() || !found.get().getUser().getId().equals(userId)) return false;
        Notification notification = found.get();
        if (!notification.isReadFlag()) {
            notification.setReadFlag(true);
            repository.save(notification);
        }
        return true;
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId);
    }

    private boolean usable(User user, Long excludeUserId) {
        return user != null && user.getId() != null && user.isActive() && !user.isDeleted()
                && (excludeUserId == null || !user.getId().equals(excludeUserId));
    }

    private String clean(String value, int max) {
        if (value == null) return "";
        String cleaned = value.replaceAll("\\p{Cntrl}+", " ").trim();
        return cleaned.length() > max ? cleaned.substring(0, max) + "..." : cleaned;
    }

    private Notification build(User user, NotificationType type, String message, Long requestId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRequestId(requestId);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
