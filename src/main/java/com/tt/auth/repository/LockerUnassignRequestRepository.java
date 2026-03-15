package com.tt.auth.repository;

import com.tt.auth.entity.LockerUnassignRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LockerUnassignRequestRepository extends JpaRepository<LockerUnassignRequest, Long> {
    List<LockerUnassignRequest> findByStatus(LockerUnassignRequest.RequestStatus status);
    Optional<LockerUnassignRequest> findByLockerIdAndStatus(Long lockerId, LockerUnassignRequest.RequestStatus status);
}
