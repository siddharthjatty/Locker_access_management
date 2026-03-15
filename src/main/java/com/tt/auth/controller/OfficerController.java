package com.tt.auth.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.tt.auth.entity.Officer;

import com.tt.auth.entity.Customer;
import com.tt.auth.entity.Locker;
import com.tt.auth.repository.CustomerRepository;
import com.tt.auth.repository.OfficerRepository;
import com.tt.auth.service.AccessRequestService;
import com.tt.auth.service.LockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/officer")
@RequiredArgsConstructor
public class OfficerController {

    private final OfficerRepository officerRepository;
    private final AccessRequestService accessRequestService;
    private final LockerService lockerService;
    private final CustomerRepository customerRepository;

    private Officer getAuthenticatedOfficer(Authentication auth) {
        return officerRepository.findByUsername(auth.getName())
                .orElseGet(() -> officerRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Officer not found")));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Officer officer = getAuthenticatedOfficer(auth);
        model.addAttribute("officer", officer);
        
        // Stats for branch
        List<Locker> branchLockers = lockerService.getLockersByBranch(officer.getBranch());
        model.addAttribute("branchLockers", branchLockers);
        model.addAttribute("userCount", branchLockers.stream()
                .filter(l -> l.getAssignedTo() != null)
                .map(l -> l.getAssignedTo().getId())
                .distinct().count());

        model.addAttribute("pendingRequests", accessRequestService.getPendingRequestsForOfficer()); // Simplified, usually filtered by branch
        model.addAttribute("activeRequests", accessRequestService.getActiveSessions());
        model.addAttribute("lockerAllocations", lockerService.getPendingAllocations());
        model.addAttribute("pendingUnassignRequests", lockerService.getPendingUnassignRequests());
        
        // Calculate counts for branch users
        Map<Long, Long> counts = branchLockers.stream()
                .filter(l -> l.getAssignedTo() != null)
                .collect(Collectors.groupingBy(l -> l.getAssignedTo().getId(), Collectors.counting()));
        model.addAttribute("lockerCounts", counts);
        
        return "officer/dashboard";
    }

    @PostMapping("/forward-request")
    public String forwardRequest(@RequestParam Long requestId) {
        lockerService.forwardRequestToAdmin(requestId);
        return "redirect:/officer/dashboard?success=forwarded";
    }

    @PostMapping("/approve-locker")
    public String approveLocker(@RequestParam Long requestId, @RequestParam Long lockerId) {
        lockerService.approveAllocation(requestId, lockerId);
        return "redirect:/officer/dashboard?success=approved";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpForm(@RequestParam Long reqId, Model model) {
        model.addAttribute("reqId", reqId);
        return "officer/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            Authentication auth,
            @RequestParam Long reqId,
            @RequestParam String otp,
            Model model) {
        try {
            Officer officer = getAuthenticatedOfficer(auth);
            accessRequestService.verifyOfficerOtp(reqId, otp, officer);
            return "redirect:/officer/dashboard?success=authorized";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reqId", reqId);
            return "officer/verify-otp";
        }
    }

    @GetMapping("/resend-otp")
    public String resendOtp(
            Authentication auth,
            @RequestParam Long reqId,
            Model model) {
        try {
            Officer officer = getAuthenticatedOfficer(auth);
            accessRequestService.resendOfficerOtp(reqId, officer);
            return "redirect:/officer/verify-otp?reqId=" + reqId + "&success=resent";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("reqId", reqId);
            return "officer/verify-otp";
        }
    }

    @PostMapping("/mark-exit")
    public String markExit(@RequestParam Long reqId) {
        accessRequestService.markExit(reqId);
        return "redirect:/officer/dashboard?success=exited";
    }

    @GetMapping("/logs")
    public String logs(Authentication auth, Model model) {
        Officer officer = getAuthenticatedOfficer(auth);
        model.addAttribute("officer", officer);
        model.addAttribute("requests", accessRequestService.getAllRequests());
        return "officer/logs";
    }

    @PostMapping("/unassign/initiate")
    public String initiateUnassign(@RequestParam Long requestId) {
        lockerService.initiateUnassignOtp(requestId);
        return "redirect:/officer/dashboard?unassignReqId=" + requestId + "&showOtp=true";
    }

    @PostMapping("/unassign/confirm")
    public String confirmUnassign(@RequestParam Long requestId, @RequestParam String otp, Model model, Authentication auth) {
        try {
            lockerService.processUnassignment(requestId, otp);
            return "redirect:/officer/dashboard?success=unassigned";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return dashboard(auth, model);
        }
    }
}
