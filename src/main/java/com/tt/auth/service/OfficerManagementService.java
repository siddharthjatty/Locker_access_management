package com.tt.auth.service;

import com.tt.auth.entity.Officer;
import com.tt.auth.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficerManagementService {

    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Officer> getAllOfficers() {
        return officerRepository.findAll();
    }

    public long getTotalOfficerCount() {
        return officerRepository.count();
    }

    @Transactional
    public Officer createOfficer(String name, String email, String username, String branch, String password) {
        if (officerRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (officerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        Officer officer = Officer.builder()
                .name(name)
                .email(email)
                .username(username)
                .branch(branch)
                .password(passwordEncoder.encode(password))
                .build();
        return officerRepository.save(officer);
    }

    @Transactional
    public void removeOfficer(Long officerId) {
        officerRepository.deleteById(officerId);
    }

    @Transactional
    public void updateOfficerBranch(Long officerId, String branch) {
        Officer officer = officerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found"));
        officer.setBranch(branch);
        officerRepository.save(officer);
    }

    @Transactional
    public void resetOfficerPassword(Long officerId, String newPassword) {
        Officer officer = officerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found"));
        officer.setPassword(passwordEncoder.encode(newPassword));
        officerRepository.save(officer);
    }
}
