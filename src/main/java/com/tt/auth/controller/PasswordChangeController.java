package com.tt.auth.controller;

import com.tt.auth.entity.Admin;
import com.tt.auth.entity.Customer;
import com.tt.auth.entity.Officer;
import com.tt.auth.entity.enums.OtpType;
import com.tt.auth.repository.AdminRepository;
import com.tt.auth.repository.CustomerRepository;
import com.tt.auth.repository.OfficerRepository;
import com.tt.auth.service.EmailService;
import com.tt.auth.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Optional;

@Controller
@RequestMapping("/change-password")
@RequiredArgsConstructor
@Slf4j
public class PasswordChangeController {

    private final CustomerRepository customerRepository;
    private final OfficerRepository officerRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    @GetMapping
    public String showOldPasswordForm() {
        return "change-password-step1";
    }

    @PostMapping("/verify-old")
    public String verifyOldPassword(@RequestParam String oldPassword, Authentication authentication, HttpSession session, Model model) {
        String username = authentication.getName();
        String currentPassword = "";
        Long userId = null;
        String email = "";

        Optional<Customer> customer = customerRepository.findByUsername(username);
        if (customer.isPresent()) {
            currentPassword = customer.get().getPassword();
            userId = customer.get().getId();
            email = customer.get().getEmail();
        } else {
            Optional<Officer> officer = officerRepository.findByUsername(username);
            if (officer.isPresent()) {
                currentPassword = officer.get().getPassword();
                userId = officer.get().getId();
                email = officer.get().getEmail();
            } else {
                Optional<Admin> admin = adminRepository.findByUsername(username);
                if (admin.isPresent()) {
                    currentPassword = admin.get().getPassword();
                    userId = admin.get().getId();
                    email = admin.get().getEmail();
                }
            }
        }

        if (userId != null && passwordEncoder.matches(oldPassword, currentPassword)) {
            String otp = otpService.generateOtp(userId, OtpType.PASSWORD_CHANGE, username);
            emailService.sendOtpEmail(email, otp, "Password Change Request");
            session.setAttribute("password_change_user_id", userId);
            session.setAttribute("password_change_email", email);
            return "redirect:/change-password/otp";
        } else {
            model.addAttribute("error", "Incorrect old password.");
            return "change-password-step1";
        }
    }

    @GetMapping("/otp")
    public String showOtpForm(HttpSession session) {
        if (session.getAttribute("password_change_user_id") == null) return "redirect:/change-password";
        return "change-password-step2";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp, HttpSession session, Model model, Authentication authentication) {
        Long userId = (Long) session.getAttribute("password_change_user_id");
        if (userId == null) return "redirect:/change-password";

        if (otpService.validateOtp(userId, OtpType.PASSWORD_CHANGE, otp, authentication.getName())) {
            session.setAttribute("otp_verified", true);
            return "redirect:/change-password/new";
        } else {
            model.addAttribute("error", "Invalid or expired OTP.");
            return "change-password-step2";
        }
    }

    @GetMapping("/new")
    public String showNewPasswordForm(HttpSession session) {
        if (session.getAttribute("otp_verified") == null) return "redirect:/change-password";
        return "change-password-step3";
    }

    @PostMapping("/update")
    public String updatePassword(@RequestParam String newPassword, @RequestParam String confirmPassword, HttpSession session, Authentication authentication, Model model) {
        if (session.getAttribute("otp_verified") == null) return "redirect:/change-password";
        Long userId = (Long) session.getAttribute("password_change_user_id");

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "change-password-step3";
        }

        String username = authentication.getName();
        String encodedPassword = passwordEncoder.encode(newPassword);

        Optional<Customer> customer = customerRepository.findById(userId);
        if (customer.isPresent()) {
            Customer c = customer.get();
            c.setPassword(encodedPassword);
            customerRepository.save(c);
        } else {
            Optional<Officer> officer = officerRepository.findById(userId);
            if (officer.isPresent()) {
                Officer o = officer.get();
                o.setPassword(encodedPassword);
                officerRepository.save(o);
            } else {
                Optional<Admin> admin = adminRepository.findById(userId);
                if (admin.isPresent()) {
                    Admin a = admin.get();
                    a.setPassword(encodedPassword);
                    adminRepository.save(a);
                }
            }
        }

        session.removeAttribute("password_change_user_id");
        session.removeAttribute("otp_verified");
        return "redirect:/login?passwordChanged";
    }
}
