package com.tt.auth.service;

import com.tt.auth.entity.Admin;
import com.tt.auth.entity.Customer;
import com.tt.auth.entity.Officer;
import com.tt.auth.repository.AdminRepository;
import com.tt.auth.repository.CustomerRepository;
import com.tt.auth.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final OfficerRepository officerRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("🔐 Attempting to load user: [{}]", username);

        // Try username first
        Optional<Customer> customerOpt = customerRepository.findByUsername(username);
        // Fallback to email if username search yields nothing
        if (customerOpt.isEmpty()) {
            customerOpt = customerRepository.findByEmail(username);
        }
        
        if (customerOpt.isPresent()) {
            Customer c = customerOpt.get();
            log.info("✅ Found Customer: [{}, {}]", c.getUsername(), c.getEmail());
            return new org.springframework.security.core.userdetails.User(
                    c.getUsername(), c.getPassword(), c.isVerified(), true, true, true,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );
        }

        Optional<Officer> officerOpt = officerRepository.findByUsername(username);
        if (officerOpt.isEmpty()) {
            officerOpt = officerRepository.findByEmail(username);
        }
        
        if (officerOpt.isPresent()) {
            Officer o = officerOpt.get();
            log.info("✅ Found Officer: [{}, {}]", o.getUsername(), o.getEmail());
            return new org.springframework.security.core.userdetails.User(
                    o.getUsername(), o.getPassword(), true, true, true, true,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_OFFICER"))
            );
        }

        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isEmpty()) {
            adminOpt = adminRepository.findByEmail(username);
        }
        
        if (adminOpt.isPresent()) {
            Admin a = adminOpt.get();
            log.info("✅ Found Admin: [{}, {}]", a.getUsername(), a.getEmail());
            return new org.springframework.security.core.userdetails.User(
                    a.getUsername(), a.getPassword(), true, true, true, true,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        log.warn("❌ User not found: [{}]", username);
        throw new UsernameNotFoundException("User not found with username or email: " + username);
    }
}
