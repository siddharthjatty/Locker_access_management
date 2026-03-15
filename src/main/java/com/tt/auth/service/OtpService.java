package com.tt.auth.service;

import com.tt.auth.entity.OtpToken;
import com.tt.auth.entity.enums.OtpType;
import com.tt.auth.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${app.otp.length}")
    private int otpLength;

    /**
     * Generate a numeric OTP, invalidate previous ones, and save.
     */
    @Transactional
    public String generateOtp(Long referenceId, OtpType otpType, String logInfo) {
        otpRepository.invalidateAllOtpForReferenceAndType(referenceId, otpType);

        String otp = generateRandomOtp();

        OtpToken otpToken = OtpToken.builder()
                .referenceId(referenceId)
                .otpType(otpType)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();

        otpRepository.save(otpToken);
        log.info("OTP {} generated for {}", otpType, logInfo);

        return otp;
    }

    /**
     * Validate the OTP.
     */
    @Transactional
    public boolean validateOtp(Long referenceId, OtpType otpType, String otpCode, String logInfo) {
        var otpTokenOpt = otpRepository.findTopByReferenceIdAndOtpTypeOrderByCreatedAtDesc(referenceId, otpType);

        if (otpTokenOpt.isEmpty()) {
            log.warn("No active OTP found for {}", logInfo);
            return false;
        }

        OtpToken otpToken = otpTokenOpt.get();
        // Always delete the token once it's retrieved for a verification attempt
        otpRepository.delete(otpToken);

        if (otpToken.isExpired()) {
            log.warn("OTP expired for {}", logInfo);
            return false;
        }

        if (!otpToken.getOtpCode().equals(otpCode)) {
            log.warn("Invalid OTP for {}", logInfo);
            return false;
        }

        log.info("OTP verified successfully for {}", logInfo);
        return true;
    }

    private String generateRandomOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int number = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", number);
    }
}
