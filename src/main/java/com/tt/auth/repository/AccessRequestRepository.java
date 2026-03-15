package com.tt.auth.repository;

import com.tt.auth.entity.AccessRequest;
import com.tt.auth.entity.enums.AccessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<AccessRequest> findByStatusOrderByCreatedAtDesc(AccessStatus status);
    boolean existsByCustomerIdAndStatusIn(Long customerId, List<AccessStatus> activeStatuses);
    List<AccessRequest> findByLockerIdOrderByCreatedAtDesc(Long lockerId);
}
