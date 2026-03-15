package com.tt.auth.repository;

import com.tt.auth.entity.Customer;
import com.tt.auth.entity.LockerAllocationRequest;
import com.tt.auth.entity.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LockerAllocationRequestRepository extends JpaRepository<LockerAllocationRequest, Long> {
    List<LockerAllocationRequest> findByStatusOrderByCreatedAtDesc(AllocationStatus status);
    Optional<LockerAllocationRequest> findByCustomerAndStatus(Customer customer, AllocationStatus status);
}
