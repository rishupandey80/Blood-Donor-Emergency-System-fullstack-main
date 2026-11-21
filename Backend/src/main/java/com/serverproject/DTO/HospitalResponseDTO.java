package com.serverproject.DTO;

import com.serverproject.enums.VerificationStatus;

public class HospitalResponseDTO {

    private Long id;
    private String name;
    private String contactNumber;
    private String registrationNumber;
    private String address;
    private String city;
    private VerificationStatus verificationStatus;
    private Long userId;

    public HospitalResponseDTO() {}

    public HospitalResponseDTO(Long id, String name, String contactNumber, String registrationNumber,
                               String address, String city, VerificationStatus verificationStatus, Long userId) {
        this.id = id;
        this.name = name;
        this.contactNumber = contactNumber;
        this.registrationNumber = registrationNumber;
        this.address = address;
        this.city = city;
        this.verificationStatus = verificationStatus;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}