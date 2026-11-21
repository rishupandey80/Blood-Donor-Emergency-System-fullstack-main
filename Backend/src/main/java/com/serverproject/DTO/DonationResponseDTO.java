package com.serverproject.DTO;

import java.time.LocalDate;

public class DonationResponseDTO {

    private Long id;
    private Long bloodRequestId;
    private LocalDate donationDate;
    private int unitsDonated;
    private String bloodGroup;
    private String location;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBloodRequestId() { return bloodRequestId; }
    public void setBloodRequestId(Long bloodRequestId) { this.bloodRequestId = bloodRequestId; }

    public LocalDate getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDate donationDate) { this.donationDate = donationDate; }

    public int getUnitsDonated() { return unitsDonated; }
    public void setUnitsDonated(int unitsDonated) { this.unitsDonated = unitsDonated; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}