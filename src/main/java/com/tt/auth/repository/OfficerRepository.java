package com.tt.auth.repository;

import com.tt.auth.entity.Officer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {
    Optional<Officer> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Officer> findByUsername(String username);
    boolean existsByUsername(String username);
}
