package com.serverproject.mapper;

import com.serverproject.DTO.DonationResponseDTO;
import com.serverproject.model.Donation;

public class DonationMapper {

    public static DonationResponseDTO toResponseDTO(Donation donation) {
        DonationResponseDTO dto = new DonationResponseDTO();
        dto.setId(donation.getId());
        dto.setBloodRequestId(donation.getBloodRequest().getId());
        dto.setDonationDate(donation.getDonationDate());
        dto.setUnitsDonated(donation.getUnitsDonated());
        dto.setBloodGroup(donation.getBloodRequest().getBloodGroup());
        dto.setLocation(donation.getBloodRequest().getLocation());
        return dto;
    }
}