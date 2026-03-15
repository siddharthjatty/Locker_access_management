package com.tt.auth.service;

import com.tt.auth.entity.AccessRequest;
import com.tt.auth.entity.Customer;
import com.tt.auth.entity.Locker;
import com.tt.auth.entity.Officer;
import com.tt.auth.entity.enums.AccessStatus;
import com.tt.auth.entity.enums.OtpType;
import com.tt.auth.repository.AccessRequestRepository;
import com.tt.auth.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;
    private final LockerService lockerService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final OfficerRepository officerRepository;

    @Transactional
    public AccessRequest createRequest(Customer customer, Long lockerId, String date, String timeSlot, String purpose) {
        // Only one active request allowed
        List<AccessStatus> activeStatuses = Arrays.asList(AccessStatus.PENDING, AccessStatus.CUSTOMER_VERIFIED, AccessStatus.OFFICER_VERIFIED);
        if (accessRequestRepository.existsByCustomerIdAndStatusIn(customer.getId(), activeStatuses)) {
            throw new RuntimeException("Customer already has an active access request.");
        }

        Locker locker = lockerService.getLockerById(lockerId);
        if (!locker.getAssignedTo().getId().equals(customer.getId())) {
            throw new RuntimeException("Locker does not belong to this customer.");
        }

        AccessRequest request = AccessRequest.builder()
                .customer(customer)
                .locker(locker)
                .requestedDate(java.time.LocalDate.parse(date))
                .requestedTimeSlot(timeSlot)
                .purpose(purpose)
                .status(AccessStatus.PENDING)
                .build();

        request = accessRequestRepository.save(request);

        // Generate and send Customer OTP
        String otp = otpService.generateOtp(request.getId(), OtpType.CUSTOMER_ACCESS, customer.getEmail());
        emailService.sendOtpEmail(customer.getEmail(), otp, customer.getName());

        return request;
    }

    @Transactional
    public void verifyCustomerOtp(Long requestId, String otpCode) {
        AccessRequest request = getRequestById(requestId);
        if (request.getStatus() != AccessStatus.PENDING) {
            throw new RuntimeException("Request is not in PENDING state.");
        }

        boolean isValid = otpService.validateOtp(request.getId(), OtpType.CUSTOMER_ACCESS, otpCode, request.getCustomer().getEmail());
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP for customer verification.");
        }

        request.setStatus(AccessStatus.CUSTOMER_VERIFIED);
        accessRequestRepository.save(request);

        // Notify officers in the branch
        List<Officer> officers = officerRepository.findAll();
        if (officers.isEmpty()) {
            throw new RuntimeException("No officers registered in the system.");
        }

        // Try to match by branch, but fall back to the first officer if no match found to ensure the demo continues
        Officer branchOfficer = officers.stream()
                .filter(o -> o.getBranch() != null && o.getBranch().equalsIgnoreCase(request.getLocker().getBranch()))
                .findFirst()
                .orElse(officers.get(0)); // Fallback to first officer

        String officerOtp = otpService.generateOtp(request.getId(), OtpType.OFFICER_AUTH, branchOfficer.getEmail());
        emailService.sendOtpEmail(branchOfficer.getEmail(), officerOtp, branchOfficer.getName());
    }

    @Transactional
    public void resendCustomerOtp(Long requestId) {
        AccessRequest request = getRequestById(requestId);
        if (request.getStatus() != AccessStatus.PENDING) {
            throw new RuntimeException("OTP can only be resent for PENDING requests.");
        }
        String otp = otpService.generateOtp(request.getId(), OtpType.CUSTOMER_ACCESS, request.getCustomer().getEmail());
        emailService.sendOtpEmail(request.getCustomer().getEmail(), otp, request.getCustomer().getName());
        log.info("Customer OTP resent for request: {}", requestId);
    }

    @Transactional
    public void resendOfficerOtp(Long requestId, Officer officer) {
        AccessRequest request = getRequestById(requestId);
        if (request.getStatus() != AccessStatus.CUSTOMER_VERIFIED) {
            throw new RuntimeException("OTP can only be resent for requests waiting for officer verification.");
        }
        String otp = otpService.generateOtp(request.getId(), OtpType.OFFICER_AUTH, officer.getEmail());
        emailService.sendOtpEmail(officer.getEmail(), otp, officer.getName());
        log.info("Officer OTP resent for request: {}", requestId);
    }

    @Transactional
    public void verifyOfficerOtp(Long requestId, String otpCode, Officer officer) {
        AccessRequest request = getRequestById(requestId);
        if (request.getStatus() != AccessStatus.CUSTOMER_VERIFIED) {
            throw new RuntimeException("Request is not waiting for officer authorization.");
        }

        boolean isValid = otpService.validateOtp(request.getId(), OtpType.OFFICER_AUTH, otpCode, officer.getEmail());
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP for officer verification.");
        }

        request.setStatus(AccessStatus.OFFICER_VERIFIED);
        request.setEntryTime(LocalDateTime.now());
        lockerService.incrementVisitCount(request.getLocker().getId());
        accessRequestRepository.save(request);
    }

    @Transactional
    public void markExit(Long requestId) {
        AccessRequest request = getRequestById(requestId);
        if (request.getStatus() != AccessStatus.OFFICER_VERIFIED) {
            throw new RuntimeException("Cannot mark exit: Access not active.");
        }

        request.setExitTime(LocalDateTime.now());
        request.setStatus(AccessStatus.COMPLETED);
        accessRequestRepository.save(request);
    }

    public AccessRequest getRequestById(Long id) {
        return accessRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Access Request not found: " + id));
    }

    public List<AccessRequest> getRequestsByCustomer(Long customerId) {
        return accessRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<AccessRequest> getPendingRequestsForOfficer() {
        return accessRequestRepository.findByStatusOrderByCreatedAtDesc(AccessStatus.CUSTOMER_VERIFIED);
    }

    public List<AccessRequest> getActiveSessions() {
        return accessRequestRepository.findByStatusOrderByCreatedAtDesc(AccessStatus.OFFICER_VERIFIED);
    }
    
    public List<AccessRequest> getAllRequests() {
        return accessRequestRepository.findAll();
    }

    public List<AccessRequest> getRecentActivity(int limit) {
        return accessRequestRepository.findAll(org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();
    }

    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    @Transactional
    public void autoExpireRequests() {
        LocalDateTime limit = LocalDateTime.now().minusMinutes(15);
        List<AccessRequest> pendingRequests = accessRequestRepository.findByStatusOrderByCreatedAtDesc(AccessStatus.PENDING);
        
        for (AccessRequest req : pendingRequests) {
            if (req.getCreatedAt().isBefore(limit)) {
                req.setStatus(AccessStatus.EXPIRED);
                accessRequestRepository.save(req);
                log.info("Access Request {} marked as EXPIRED due to timeout.", req.getId());
            }
        }
    }
}
