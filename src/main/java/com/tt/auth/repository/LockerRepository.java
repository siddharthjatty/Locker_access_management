package com.tt.auth.repository;

import com.tt.auth.entity.Locker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LockerRepository extends JpaRepository<Locker, Long> {
    List<Locker> findByAssignedToId(Long customerId);
    List<Locker> findByBranch(String branch);
    long countByStatus(com.tt.auth.entity.enums.LockerStatus status);
}
