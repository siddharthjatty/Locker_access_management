package com.tt.auth.repository;

import com.tt.auth.entity.OtpToken;
import com.tt.auth.entity.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByReferenceIdAndOtpTypeOrderByCreatedAtDesc(Long referenceId, OtpType otpType);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.referenceId = :referenceId AND o.otpType = :otpType")
    void invalidateAllOtpForReferenceAndType(@Param("referenceId") Long referenceId, @Param("otpType") OtpType otpType);
}
