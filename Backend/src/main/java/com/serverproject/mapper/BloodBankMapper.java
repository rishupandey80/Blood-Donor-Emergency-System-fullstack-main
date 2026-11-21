package com.serverproject.mapper;

import com.serverproject.DTO.BloodBankRequestDTO;
import com.serverproject.DTO.BloodBankResponseDTO;
import com.serverproject.DTO.BloodStockResponseDTO;
import com.serverproject.model.BloodBank;
import com.serverproject.model.BloodStock;

public class BloodBankMapper {

    public static BloodBank toEntity(BloodBankRequestDTO dto) {
        BloodBank bank = new BloodBank();
        bank.setName(dto.getName());
        bank.setContactNumber(dto.getContactNumber());
        bank.setLicenseNumber(dto.getLicenseNumber());
        bank.setAddress(dto.getAddress());
        bank.setCity(dto.getCity());
        return bank;
    }

    public static BloodBankResponseDTO toResponseDTO(BloodBank bank) {
        BloodBankResponseDTO dto = new BloodBankResponseDTO();
        dto.setId(bank.getId());
        dto.setName(bank.getName());
        dto.setContactNumber(bank.getContactNumber());
        dto.setLicenseNumber(bank.getLicenseNumber());
        dto.setAddress(bank.getAddress());
        dto.setCity(bank.getCity());
        dto.setVerificationStatus(bank.getVerificationStatus());
        dto.setUserId(bank.getUser() != null ? bank.getUser().getId() : null);
        return dto;
    }

    public static BloodStockResponseDTO toStockResponseDTO(BloodStock stock) {
        BloodStockResponseDTO dto = new BloodStockResponseDTO();
        dto.setId(stock.getId());
        dto.setBloodBankId(stock.getBloodBank().getId());
        dto.setBloodBankName(stock.getBloodBank().getName());
        dto.setCity(stock.getBloodBank().getCity());
        dto.setContactNumber(stock.getBloodBank().getContactNumber());
        dto.setBloodGroup(stock.getBloodGroup());
        dto.setUnitsAvailable(stock.getUnitsAvailable());
        return dto;
    }
}