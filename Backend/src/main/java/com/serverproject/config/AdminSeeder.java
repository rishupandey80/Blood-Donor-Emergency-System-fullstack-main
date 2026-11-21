package com.serverproject.config;

import com.serverproject.enums.Role;
import com.serverproject.model.User;
import com.serverproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.default.email}")
    private String adminEmail;

    @Value("${admin.default.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        boolean adminExists = userRepository.findByEmail(adminEmail).isPresent();

        if (adminExists) {
            System.out.println("Admin already exists, skipping seed.");
            return;
        }

        User admin = new User();
        admin.setName("Super Admin");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setPhone("0000000000");
        admin.setGender("Other");
        admin.setRole(Role.ADMIN);
        admin.setVerified(true);

        userRepository.save(admin);
        System.out.println("Default Admin created: " + adminEmail);
    }
}