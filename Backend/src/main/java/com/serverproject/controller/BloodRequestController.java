package com.serverproject.controller;

import com.serverproject.DTO.BloodRequestDTO;
import com.serverproject.DTO.BloodRequestResponseDTO;
import com.serverproject.DTO.EligibilityResponseDTO;
import com.serverproject.enums.RequestStatus;
import com.serverproject.enums.Role;
import com.serverproject.mapper.BloodRequestMapper;
import com.serverproject.model.BloodRequest;
import com.serverproject.model.User;
import com.serverproject.service.BloodRequestService;
import com.serverproject.util.BloodCompatibility;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/request")
public class BloodRequestController {

    @Autowired
    private BloodRequestService service;

    @PostMapping
    public ResponseEntity<?> createRequest(Authentication auth, @Valid @RequestBody BloodRequestDTO dto) {
        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found!");
        }
        if (user.get().getRole() != Role.USER && user.get().getRole() != Role.HOSPITAL) {
            return ResponseEntity.status(403).body("Only USER and HOSPITAL accounts can create blood requests.");
        }

        BloodRequest saved = service.createRequest(user.get(), dto);
        return ResponseEntity.ok(BloodRequestMapper.toResponseDTO(saved));
    }

    @GetMapping
    public ResponseEntity<?> getAllRequests(Authentication auth) {
        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) return ResponseEntity.status(401).body("User not found!");
        if (user.get().getRole() != Role.ADMIN) return ResponseEntity.status(403).body("Admin access required.");

        return ResponseEntity.ok(service.getAllRequests().stream()
                .map(BloodRequestMapper::toResponseDTO).collect(Collectors.toList()));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveRequests(Authentication auth) {
        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) return ResponseEntity.status(401).body("User not found!");

        if (user.get().getRole() != Role.DONOR && user.get().getRole() != Role.BLOOD_BANK) {
            return ResponseEntity.status(403).body("Only donors and verified blood banks can view incoming requests.");
        }

        List<BloodRequestResponseDTO> list = service.getActiveRequestsFor(user.get()).stream()
                .map(BloodRequestMapper::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) return ResponseEntity.status(401).body("User not found!");
        if (user.get().getRole() != Role.USER && user.get().getRole() != Role.HOSPITAL) {
            return ResponseEntity.status(403).body("This feed is only for request creators.");
        }

        List<BloodRequestResponseDTO> list = service.getMyRequestsFor(user.get()).stream()
                .map(BloodRequestMapper::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/accepted-by-me")
    public ResponseEntity<?> getAcceptedByMe(Authentication auth) {
        Optional<User> user = service.findUserByEmail(auth.getName());
        if (user.isEmpty()) return ResponseEntity.status(401).body("User not found!");
        if (user.get().getRole() != Role.DONOR && user.get().getRole() != Role.BLOOD_BANK) {
            return ResponseEntity.status(403).body("Only donors and blood banks can view requests accepted by them.");
        }

        List<BloodRequestResponseDTO> list = service.getAcceptedRequestsFor(user.get()).stream()
                .map(BloodRequestMapper::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id, Authentication auth) {
        Optional<User> accepter = service.findUserByEmail(auth.getName());
        Optional<BloodRequest> optionalRequest = service.findById(id);
        if (accepter.isEmpty() || optionalRequest.isEmpty()) {
            return ResponseEntity.status(404).body("Request or user not found!");
        }

        User actor = accepter.get();
        BloodRequest request = optionalRequest.get();
        if (actor.getRole() != Role.DONOR && actor.getRole() != Role.BLOOD_BANK) {
            return ResponseEntity.status(403).body("Only donors and blood banks can accept requests.");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            return ResponseEntity.badRequest().body("Only PENDING requests can be accepted. Current status: " + request.getStatus());
        }
        if (isOwner(request, actor)) {
            return ResponseEntity.badRequest().body("You cannot accept your own request!");
        }

        if (actor.getRole() == Role.DONOR) {
            EligibilityResponseDTO eligibility = service.donorEligibility(actor);
            if (eligibility == null) {
                return ResponseEntity.status(403).body("Create your donor profile first!");
            }
            if (!eligibility.isEligible()) {
                return ResponseEntity.status(403).body("You are not eligible to donate right now: "
                        + String.join("; ", eligibility.getReasons()));
            }
            if (!service.isBloodCompatible(actor, request)) {
                return ResponseEntity.status(403).body("Blood group not compatible. A " + request.getBloodGroup()
                        + " request can be served by donor groups: "
                        + BloodCompatibility.compatibleDonorGroups(request.getBloodGroup()));
            }
        } else {
            if (!service.isVerifiedBloodBank(actor)) {
                return ResponseEntity.status(403).body("Only VERIFIED blood banks can accept requests.");
            }
            if (!service.hasCompatibleStock(actor, request)) {
                return ResponseEntity.status(409).body("Your blood bank does not have enough compatible stock for this request. Update inventory and try again.");
            }
        }

        return ResponseEntity.ok(BloodRequestMapper.toResponseDTO(service.accept(request, actor)));
    }

    @PutMapping("/{id}/fulfill")
    public ResponseEntity<?> fulfillRequest(@PathVariable Long id, Authentication auth) {
        Optional<User> user = service.findUserByEmail(auth.getName());
        Optional<BloodRequest> optionalRequest = service.findById(id);
        if (user.isEmpty() || optionalRequest.isEmpty()) {
            return ResponseEntity.status(404).body("Request or user not found!");
        }

        BloodRequest request = optionalRequest.get();
        if (!isOwnerOrAdmin(request, user.get())) {
            return ResponseEntity.status(403).body("Only the requester or an admin can mark this as fulfilled!");
        }
        if (request.getStatus() != RequestStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body("Only ACCEPTED requests can be fulfilled. Current status: " + request.getStatus());
        }

        return ResponseEntity.ok(BloodRequestMapper.toResponseDTO(service.fulfill(request, user.get())));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRequest(@PathVariable Long id, Authentication auth) {
        Optional<User> user = service.findUserByEmail(auth.getName());
        Optional<BloodRequest> optionalRequest = service.findById(id);
        if (user.isEmpty() || optionalRequest.isEmpty()) {
            return ResponseEntity.status(404).body("Request or user not found!");
        }

        BloodRequest request = optionalRequest.get();
        if (!isOwnerOrAdmin(request, user.get())) {
            return ResponseEntity.status(403).body("Only the requester or an admin can cancel this request!");
        }
        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body("Only PENDING or ACCEPTED requests can be canceled. Current status: " + request.getStatus());
        }

        return ResponseEntity.ok(BloodRequestMapper.toResponseDTO(service.cancel(request, user.get())));
    }

    private boolean isOwner(BloodRequest request, User user) {
        return request.getRequestedBy() != null && request.getRequestedBy().getId().equals(user.getId());
    }

    private boolean isOwnerOrAdmin(BloodRequest request, User user) {
        return isOwner(request, user) || user.getRole() == Role.ADMIN;
    }
}
