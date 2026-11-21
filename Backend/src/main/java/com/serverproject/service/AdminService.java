package com.serverproject.service;

import com.serverproject.model.User;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findUser(Long id) {
        return userRepository.findById(id);
    }

    public User updateActiveStatus(User user, boolean active) {
        user.setActive(active);
        return userRepository.save(user);
    }
}