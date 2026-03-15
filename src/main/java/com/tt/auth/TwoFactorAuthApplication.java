package com.tt.auth;

import com.tt.auth.entity.Admin;
import com.tt.auth.entity.Officer;
import com.tt.auth.repository.AdminRepository;
import com.tt.auth.repository.CustomerRepository;
import com.tt.auth.repository.OfficerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class TwoFactorAuthApplication {

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(TwoFactorAuthApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner dataLoader(AdminRepository adminRepo, OfficerRepository officerRepo, CustomerRepository customerRepo, PasswordEncoder encoder, DataSource dataSource) {
        return args -> {
            // Fix NULL values in existing DB rows
            try (var conn = dataSource.getConnection()) {
                conn.createStatement().executeUpdate("UPDATE lockers SET visit_count = 0 WHERE visit_count IS NULL");
                conn.createStatement().executeUpdate("UPDATE locker_allocation_requests SET forwarded_to_admin = false WHERE forwarded_to_admin IS NULL");
                conn.createStatement().executeUpdate("UPDATE officers SET username = CONCAT('officer_', id) WHERE username IS NULL");
            } catch (Exception e) {
                System.out.println("⚠️ DB cleanup note: " + e.getMessage());
            }

            // Seed/Reset Admin
            String emailAdmin = "admin@example.com";
            Optional<Admin> adminOpt = adminRepo.findByEmail(emailAdmin);
            if (adminOpt.isPresent()) {
                Admin a = adminOpt.get();
                a.setUsername("admin");
                a.setPassword(encoder.encode("password"));
                adminRepo.save(a);
            } else {
                adminRepo.save(Admin.builder()
                        .name("System Admin")
                        .email(emailAdmin)
                        .username("admin")
                        .password(encoder.encode("password"))
                        .build());
            }

            // Seed/Reset Officers
            String[][] officerSeeds = {
                {"siddharthajatty@gmail.com", "officer1"},
                {"officer@example.com", "officer2"}
            };
            
            for (String[] seed : officerSeeds) {
                String email = seed[0];
                String username = seed[1];
                Optional<Officer> officerOpt = officerRepo.findByEmail(email);
                if (officerOpt.isPresent()) {
                    Officer o = officerOpt.get();
                    o.setUsername(username);
                    o.setPassword(encoder.encode("password"));
                    officerRepo.save(o);
                } else {
                    officerRepo.save(Officer.builder()
                            .name("Branch Officer")
                            .email(email)
                            .username(username)
                            .branch("Main Branch")
                            .password(encoder.encode("password"))
                            .build());
                }
            }

            // Cleanup for existing customers after schema shift
            customerRepo.findAll().forEach(c -> {
                if (c.getUsername() == null) {
                    c.setUsername(c.getEmail().split("@")[0] + "_" + c.getId());
                    customerRepo.save(c);
                }
            });
        };
    }
}
