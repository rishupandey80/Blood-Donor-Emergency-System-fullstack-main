package com.serverproject.mapper;

import com.serverproject.DTO.HospitalRequestDTO;
import com.serverproject.DTO.HospitalResponseDTO;
import com.serverproject.model.Hospital;

public class HospitalMapper {

    public static Hospital toEntity(HospitalRequestDTO dto) {
        Hospital hospital = new Hospital();
        hospital.setName(dto.getName());
        hospital.setContactNumber(dto.getContactNumber());
        hospital.setRegistrationNumber(dto.getRegistrationNumber());
        hospital.setAddress(dto.getAddress());
        hospital.setCity(dto.getCity());
        return hospital;
    }

    public static HospitalResponseDTO toResponseDTO(Hospital hospital) {
        return new HospitalResponseDTO(
                hospital.getId(),
                hospital.getName(),
                hospital.getContactNumber(),
                hospital.getRegistrationNumber(),
                hospital.getAddress(),
                hospital.getCity(),
                hospital.getVerificationStatus(),
                hospital.getUser() != null ? hospital.getUser().getId() : null
        );
    }
}