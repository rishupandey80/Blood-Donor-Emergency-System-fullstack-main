package com.serverproject.service;

import com.serverproject.DTO.BloodBankRequestDTO;
import com.serverproject.enums.VerificationStatus;
import com.serverproject.mapper.BloodBankMapper;
import com.serverproject.model.BloodBank;
import com.serverproject.model.BloodStock;
import com.serverproject.model.User;
import com.serverproject.repository.BloodBankRepository;
import com.serverproject.repository.BloodStockRepository;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BloodBankService {

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodStockRepository stockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean hasProfile(Long userId) {
        return bloodBankRepository.existsByUserId(userId);
    }

    public boolean licenseTaken(String licenseNumber) {
        return bloodBankRepository.existsByLicenseNumber(licenseNumber);
    }

    public Optional<BloodBank> findByUserId(Long userId) {
        return bloodBankRepository.findByUserId(userId);
    }

    public BloodBank register(User user, BloodBankRequestDTO dto) {
        BloodBank bank = BloodBankMapper.toEntity(dto);
        bank.setUser(user);
        bank.setVerificationStatus(VerificationStatus.PENDING);
        return bloodBankRepository.save(bank);
    }

    public List<BloodBank> getAll() {
        return bloodBankRepository.findAll();
    }

    public List<BloodBank> searchByCity(String city) {
        return bloodBankRepository.findByCity(city);
    }

    public BloodBank updateVerificationStatus(Long id, VerificationStatus status) {
        BloodBank bank = bloodBankRepository.findById(id).orElse(null);
        if (bank != null) {
            bank.setVerificationStatus(status);
            bloodBankRepository.save(bank);
            notificationService.notifyProfileStatus(bank.getUser(), "blood bank", status);
        }
        return bank;
    }

    public BloodStock setStock(BloodBank bank, String bloodGroup, int units) {
        BloodStock stock = stockRepository.findByBloodBankIdAndBloodGroup(bank.getId(), bloodGroup)
                .orElseGet(() -> {
                    BloodStock s = new BloodStock();
                    s.setBloodBank(bank);
                    s.setBloodGroup(bloodGroup);
                    return s;
                });
        stock.setUnitsAvailable(units);
        return stockRepository.save(stock);
    }

    public List<BloodStock> getStock(Long bankId) {
        return stockRepository.findByBloodBankId(bankId);
    }

    public List<BloodStock> searchStock(String bloodGroup, String city) {
        return stockRepository.searchAvailable(bloodGroup, city, VerificationStatus.VERIFIED);
    }
}