package com.serverproject.DTO;

import com.serverproject.enums.VerificationStatus;

import java.time.LocalDate;

public class DonorResponseDTO {

    private Long id;
    private String name;
    private String bloodGroup;
    private String city;
    private String phone;
    private String email;
    private String address;
    private LocalDate lastDonationDate;
    private int age;
    private double latitude;
    private double longitude;
    private boolean available;
    private double weight;
    private VerificationStatus verificationStatus;
    private Long userId;

    public DonorResponseDTO() {}

    public DonorResponseDTO(Long id, String name, String bloodGroup, String city, String phone,
                            String email, String address, LocalDate lastDonationDate, int age,
                            double latitude, double longitude, boolean available,
                            double weight, VerificationStatus verificationStatus, Long userId) {
        this.id = id;
        this.name = name;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.lastDonationDate = lastDonationDate;
        this.age = age;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
        this.weight = weight;
        this.verificationStatus = verificationStatus;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(LocalDate lastDonationDate) { this.lastDonationDate = lastDonationDate; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}