package com.tt.auth.controller;

import com.tt.auth.entity.Locker;
import com.tt.auth.entity.enums.LockerSize;
import com.tt.auth.entity.enums.LockerStatus;
import com.tt.auth.repository.CustomerRepository;
import com.tt.auth.service.AccessRequestService;
import com.tt.auth.service.LockerService;
import com.tt.auth.entity.Officer;
import com.tt.auth.service.OfficerManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LockerService lockerService;
    private final AccessRequestService accessRequestService;
    private final CustomerRepository customerRepository;
    private final OfficerManagementService officerManagementService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        
        // Stats
        model.addAttribute("totalLockers", lockerService.getTotalLockerCount());
        model.addAttribute("availableLockers", lockerService.getAvailableLockerCount());
        model.addAttribute("totalOfficers", officerManagementService.getTotalOfficerCount());
        model.addAttribute("totalCustomers", customerRepository.count());
        model.addAttribute("pendingRequestsCount", lockerService.getPendingAllocations().size());
        
        // Recent Activity
        model.addAttribute("recentActivity", accessRequestService.getRecentActivity(5));
        
        return "admin/dashboard";
    }


    @GetMapping("/manage-lockers")
    public String manageLockers(Model model) {
        model.addAttribute("lockers", lockerService.getAllLockers());
        model.addAttribute("customers", customerRepository.findAll());
        return "admin/manage-lockers";
    }

    @PostMapping("/add-locker")
    public String addLocker(
            @RequestParam String lockerNumber,
            @RequestParam LockerSize size,
            @RequestParam String branch) {
        
        Locker locker = Locker.builder()
                .lockerNumber(lockerNumber)
                .size(size)
                .branch(branch)
                .status(LockerStatus.AVAILABLE)
                .build();
        lockerService.createLocker(locker);
        return "redirect:/admin/manage-lockers?success=added";
    }

    @PostMapping("/assign-locker")
    public String assignLocker(
            @RequestParam Long lockerId,
            @RequestParam Long customerId) {
        
        customerRepository.findById(customerId).ifPresent(customer -> 
            lockerService.assignLocker(lockerId, customer)
        );
        return "redirect:/admin/manage-lockers?success=assigned";
    }

    @GetMapping("/allocation-requests")
    public String allocationRequests(Model model) {
        model.addAttribute("requests", lockerService.getPendingAllocations());
        model.addAttribute("lockers", lockerService.getAllLockers());
        return "admin/allocation-requests";
    }

    @PostMapping("/approve-allocation")
    public String approveAllocation(
            @RequestParam Long requestId,
            @RequestParam Long lockerId) {
        lockerService.approveAllocation(requestId, lockerId);
        return "redirect:/admin/allocation-requests?success=approved";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("requests", accessRequestService.getAllRequests());
        return "admin/audit-logs";
    }

    // ========== Officer Management ========== //
    @GetMapping("/manage-officers")
    public String manageOfficers(Model model) {
        model.addAttribute("officers", officerManagementService.getAllOfficers());
        return "admin/manage-officers";
    }

    @PostMapping("/manage-officers/add")
    public String addOfficer(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String branch,
            @RequestParam String password) {
        try {
            officerManagementService.createOfficer(name, email, username, branch, password);
            return "redirect:/admin/manage-officers?success=added";
        } catch (RuntimeException e) {
            return "redirect:/admin/manage-officers?error=" + e.getMessage();
        }
    }

    @PostMapping("/manage-officers/delete")
    public String removeOfficer(@RequestParam Long id) {
        officerManagementService.removeOfficer(id);
        return "redirect:/admin/manage-officers?success=removed";
    }

    @PostMapping("/update-officer-branch")
    public String updateOfficerBranch(@RequestParam Long officerId, @RequestParam String branch) {
        officerManagementService.updateOfficerBranch(officerId, branch);
        return "redirect:/admin/manage-officers?success=updated";
    }

    @PostMapping("/reset-officer-password")
    public String resetOfficerPassword(@RequestParam Long officerId, @RequestParam String newPassword) {
        officerManagementService.resetOfficerPassword(officerId, newPassword);
        return "redirect:/admin/manage-officers?success=reset";
    }

    @PostMapping("/grant-locker-direct")
    public String grantLockerDirect(
            @RequestParam Long customerId,
            @RequestParam Long lockerId) {
        customerRepository.findById(customerId).ifPresent(customer -> 
            lockerService.assignLocker(lockerId, customer)
        );
        return "redirect:/admin/manage-lockers?success=granted";
    }
}
