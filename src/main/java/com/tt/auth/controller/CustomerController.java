package com.tt.auth.controller;

import com.tt.auth.entity.Customer;
import com.tt.auth.repository.CustomerRepository;
import com.tt.auth.service.AccessRequestService;
import com.tt.auth.service.LockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.tt.auth.service.OtpService;
import com.tt.auth.service.EmailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final LockerService lockerService;
    private final AccessRequestService accessRequestService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private Customer getAuthenticatedCustomer(Authentication auth) {
        return customerRepository.findByUsername(auth.getName())
                .orElseGet(() -> customerRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Customer not found")));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Customer customer = getAuthenticatedCustomer(auth);
        model.addAttribute("customer", customer);
        model.addAttribute("lockers", lockerService.getLockersByCustomerId(customer.getId()));
        model.addAttribute("requests", accessRequestService.getRequestsByCustomer(customer.getId()));
        
        lockerService.getPendingAllocationForCustomer(customer)
                .ifPresent(r -> model.addAttribute("pendingAllocation", r));

        return "customer/dashboard";
    }

    @PostMapping("/request-allocation")
    public String requestAllocation(
            Authentication auth,
            @RequestParam com.tt.auth.entity.enums.LockerSize size,
            Model model) {
        try {
            Customer customer = getAuthenticatedCustomer(auth);
            lockerService.submitAllocationRequest(customer, size);
            return "redirect:/customer/dashboard?success=allocation_requested";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return dashboard(auth, model); // re-render dashboard with error
        }
    }

    @GetMapping("/request-access")
    public String requestAccessForm(Authentication auth, Model model) {
        Customer customer = getAuthenticatedCustomer(auth);
        model.addAttribute("lockers", lockerService.getLockersByCustomerId(customer.getId()));
        return "customer/request-access";
    }

    @PostMapping("/request-access")
    public String submitAccessRequest(
            Authentication auth,
            @RequestParam Long lockerId,
            @RequestParam String date,
            @RequestParam String timeSlot,
            @RequestParam String purpose,
            Model model) {
        
        try {
            Customer customer = getAuthenticatedCustomer(auth);
            var req = accessRequestService.createRequest(customer, lockerId, date, timeSlot, purpose);
            return "redirect:/customer/verify-otp?reqId=" + req.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return requestAccessForm(auth, model);
        }
    }

    @GetMapping("/verify-otp")
    public String verifyOtpForm(@RequestParam Long reqId, Model model) {
        model.addAttribute("reqId", reqId);
        return "customer/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam Long reqId,
            @RequestParam String otp,
            Model model) {
        try {
            accessRequestService.verifyCustomerOtp(reqId, otp);
            return "redirect:/customer/dashboard?success=verified";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reqId", reqId);
            return "customer/verify-otp";
        }
    }

    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam Long reqId, Model model) {
        try {
            accessRequestService.resendCustomerOtp(reqId);
            return "redirect:/customer/verify-otp?reqId=" + reqId + "&success=resent";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reqId", reqId);
            return "customer/verify-otp";
        }
    }

    @GetMapping("/report")
    public String reportForm(Authentication auth, Model model) {
        Customer customer = getAuthenticatedCustomer(auth);
        model.addAttribute("customer", customer);
        model.addAttribute("lockers", lockerService.getLockersByCustomerId(customer.getId()));
        return "customer/report";
    }


    // ========== Password Change (with OTP) ========== //
    @PostMapping("/request-password-change")
    public String requestPasswordChange(Authentication auth, Model model) {
        Customer customer = getAuthenticatedCustomer(auth);
        String otp = otpService.generateOtp(customer.getId(), com.tt.auth.entity.enums.OtpType.PASSWORD_RESET, customer.getEmail());
        emailService.sendOtpEmail(customer.getEmail(), otp, customer.getName());
        model.addAttribute("info", "OTP sent to your email for password change.");
        return dashboard(auth, model);
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication auth, @RequestParam String otp, @RequestParam String newPassword, Model model) {
        Customer customer = getAuthenticatedCustomer(auth);
        boolean valid = otpService.validateOtp(customer.getId(), com.tt.auth.entity.enums.OtpType.PASSWORD_RESET, otp, customer.getEmail());
        if (!valid) {
            model.addAttribute("error", "Invalid or expired OTP.");
            return dashboard(auth, model);
        }
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);
        model.addAttribute("success", "Password changed successfully.");
        return dashboard(auth, model);
    }

    // ========== Username Change ========== //
    @PostMapping("/change-username")
    public String changeUsername(Authentication auth, @RequestParam String newUsername, Model model) {
        Customer customer = getAuthenticatedCustomer(auth);
        if (!newUsername.matches("^[A-Za-z0-9!@#$%^&*()_+=\\-{}\\[\\]:;,.?]+$")) {
            model.addAttribute("error", "Username contains invalid characters.");
            return dashboard(auth, model);
        }
        if (customerRepository.existsByUsername(newUsername)) {
            model.addAttribute("error", "Username already taken.");
            return dashboard(auth, model);
        }
        
        // Update username in DB
        customer.setUsername(newUsername);
        customerRepository.save(customer);

        // Crucial: Update the SecurityContext so the session knows about the new username
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                newUsername, auth.getCredentials(), auth.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

        return "redirect:/customer/dashboard?success=username_changed";
    }

    @PostMapping("/request-unassign")
    public String requestUnassign(Authentication auth, @RequestParam Long lockerId, Model model) {
        try {
            Customer customer = getAuthenticatedCustomer(auth);
            lockerService.submitUnassignRequest(customer, lockerId);
            return "redirect:/customer/dashboard?success=unassign_requested";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return dashboard(auth, model);
        }
    }
}
