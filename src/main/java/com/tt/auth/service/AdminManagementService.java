package com.tt.auth.service;

import com.tt.auth.entity.Admin;
import com.tt.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Transactional
    public Admin createAdmin(String name, String email, String username, String password) {
        if (adminRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (adminRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        Admin admin = Admin.builder()
                .name(name)
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(password))
                .build();
        
        return adminRepository.save(admin);
    }

    @Transactional
    public void removeAdmin(Long adminId, String currentUsername) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (admin.getUsername().equals(currentUsername)) {
            throw new RuntimeException("Cannot remove your own administrative account");
        }
        
        adminRepository.deleteById(adminId);
    }
}
