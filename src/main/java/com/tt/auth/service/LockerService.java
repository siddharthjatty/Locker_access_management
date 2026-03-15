package com.tt.auth.service;

import com.tt.auth.entity.Locker;
import com.tt.auth.entity.enums.LockerStatus;
import com.tt.auth.repository.LockerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LockerService {

    private final LockerRepository lockerRepository;
    private final com.tt.auth.repository.LockerAllocationRequestRepository allocationRepo;
    private final com.tt.auth.repository.LockerUnassignRequestRepository unassignRepo;
    private final OtpService otpService;
    private final EmailService emailService;

    public List<Locker> getAllLockers() {
        return lockerRepository.findAll();
    }

    public long getTotalLockerCount() {
        return lockerRepository.count();
    }

    public long getAvailableLockerCount() {
        return lockerRepository.countByStatus(LockerStatus.AVAILABLE);
    }

    public List<Locker> getLockersByCustomerId(Long customerId) {
        return lockerRepository.findByAssignedToId(customerId);
    }

    public Locker getLockerById(Long id) {
        return lockerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Locker not found with ID: " + id));
    }

    @Transactional
    public Locker createLocker(Locker locker) {
        return lockerRepository.save(locker);
    }

    @Transactional
    public Locker updateLockerStatus(Long id, LockerStatus status) {
        Locker locker = getLockerById(id);
        locker.setStatus(status);
        return lockerRepository.save(locker);
    }
    
    @Transactional
    public Locker assignLocker(Long lockerId, com.tt.auth.entity.Customer customer) {
        Locker locker = getLockerById(lockerId);
        locker.setAssignedTo(customer);
        locker.setStatus(LockerStatus.ASSIGNED);
        locker.setAssignedAt(java.time.LocalDateTime.now());
        return lockerRepository.save(locker);
    }

    @Transactional
    public void incrementVisitCount(Long lockerId) {
        Locker locker = getLockerById(lockerId);
        int current = locker.getVisitCount() != null ? locker.getVisitCount() : 0;
        locker.setVisitCount(current + 1);
        lockerRepository.save(locker);
    }

    public List<Locker> getLockersByBranch(String branch) {
        return lockerRepository.findByBranch(branch);
    }

    @Transactional
    public void deleteLocker(Long id) {
        lockerRepository.deleteById(id);
    }
    
    @Transactional
    public com.tt.auth.entity.LockerAllocationRequest submitAllocationRequest(com.tt.auth.entity.Customer customer, com.tt.auth.entity.enums.LockerSize size) {
        // Ensure no pending request exists
        allocationRepo.findByCustomerAndStatus(customer, com.tt.auth.entity.enums.AllocationStatus.PENDING)
            .ifPresent(r -> { throw new RuntimeException("You already have a pending allocation request."); });

        com.tt.auth.entity.LockerAllocationRequest req = com.tt.auth.entity.LockerAllocationRequest.builder()
            .customer(customer)
            .requestedSize(size)
            .status(com.tt.auth.entity.enums.AllocationStatus.PENDING)
            .build();
        return allocationRepo.save(req);
    }

    public List<com.tt.auth.entity.LockerAllocationRequest> getPendingAllocations() {
        return allocationRepo.findByStatusOrderByCreatedAtDesc(com.tt.auth.entity.enums.AllocationStatus.PENDING);
    }
    
    public Optional<com.tt.auth.entity.LockerAllocationRequest> getPendingAllocationForCustomer(com.tt.auth.entity.Customer customer) {
        return allocationRepo.findByCustomerAndStatus(customer, com.tt.auth.entity.enums.AllocationStatus.PENDING);
    }

    public List<com.tt.auth.entity.LockerAllocationRequest> getForwardedAllocations() {
        return allocationRepo.findByStatusOrderByCreatedAtDesc(com.tt.auth.entity.enums.AllocationStatus.PENDING)
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getForwardedToAdmin()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void forwardRequestToAdmin(Long requestId) {
        com.tt.auth.entity.LockerAllocationRequest req = allocationRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setForwardedToAdmin(true);
        req.setForwardedAt(java.time.LocalDateTime.now());
        allocationRepo.save(req);
    }

    @Transactional
    public void approveAllocation(Long requestId, Long lockerId) {
        com.tt.auth.entity.LockerAllocationRequest req = allocationRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        req.setStatus(com.tt.auth.entity.enums.AllocationStatus.APPROVED);
        allocationRepo.save(req);

        assignLocker(lockerId, req.getCustomer());
    }

    // ========== Locker Unassignment Flow ========== //

    @Transactional
    public com.tt.auth.entity.LockerUnassignRequest submitUnassignRequest(com.tt.auth.entity.Customer customer, Long lockerId) {
        Locker locker = getLockerById(lockerId);
        if (!customer.getId().equals(locker.getAssignedTo().getId())) {
            throw new RuntimeException("You are not the owner of this locker.");
        }

        unassignRepo.findByLockerIdAndStatus(lockerId, com.tt.auth.entity.LockerUnassignRequest.RequestStatus.PENDING)
            .ifPresent(r -> { throw new RuntimeException("A pending unassign request already exists for this locker."); });

        com.tt.auth.entity.LockerUnassignRequest req = com.tt.auth.entity.LockerUnassignRequest.builder()
            .customer(customer)
            .locker(locker)
            .status(com.tt.auth.entity.LockerUnassignRequest.RequestStatus.PENDING)
            .build();
        return unassignRepo.save(req);
    }

    public List<com.tt.auth.entity.LockerUnassignRequest> getPendingUnassignRequests() {
        return unassignRepo.findByStatus(com.tt.auth.entity.LockerUnassignRequest.RequestStatus.PENDING);
    }

    @Transactional
    public void initiateUnassignOtp(Long requestId) {
        com.tt.auth.entity.LockerUnassignRequest req = unassignRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        String otp = otpService.generateOtp(req.getCustomer().getId(), com.tt.auth.entity.enums.OtpType.UNASSIGN_CONFIRMATION, req.getCustomer().getUsername());
        emailService.sendOtpEmail(req.getCustomer().getEmail(), otp, "Locker Unassignment Confirmation");
    }

    @Transactional
    public void processUnassignment(Long requestId, String otp) {
        com.tt.auth.entity.LockerUnassignRequest req = unassignRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!otpService.validateOtp(req.getCustomer().getId(), com.tt.auth.entity.enums.OtpType.UNASSIGN_CONFIRMATION, otp, req.getCustomer().getUsername())) {
            throw new RuntimeException("Invalid or expired OTP.");
        }

        Locker locker = req.getLocker();
        String branch = locker.getBranch();
        String lockerNum = locker.getLockerNumber();

        // Perform unassignment
        locker.setAssignedTo(null);
        locker.setAssignedAt(null);
        locker.setStatus(LockerStatus.AVAILABLE);
        lockerRepository.save(locker);

        // Update request status
        req.setStatus(com.tt.auth.entity.LockerUnassignRequest.RequestStatus.APPROVED);
        unassignRepo.save(req);
    }
}
