package com.tt.auth.controller;

import com.tt.auth.entity.Customer;
import com.tt.auth.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerCustomer(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String phone,
            @RequestParam String password,
            Model model) {

        if (customerRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email is already registered.");
            return "register";
        }
        if (customerRepository.existsByUsername(username)) {
            model.addAttribute("error", "Username is already taken.");
            return "register";
        }

        Customer customer = Customer.builder()
                .name(name)
                .email(email)
                .username(username)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .isVerified(true)
                .build();

        customerRepository.save(customer);

        return "redirect:/login?registered";
    }
}
