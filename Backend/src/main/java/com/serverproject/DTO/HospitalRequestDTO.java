package com.serverproject.DTO;

import jakarta.validation.constraints.NotBlank;

public class HospitalRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "contactNumber is required")
    private String contactNumber;

    @NotBlank(message = "registrationNumber is required")
    private String registrationNumber;

    @NotBlank(message = "address is required")
    private String address;

    @NotBlank(message = "city is required")
    private String city;

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
}