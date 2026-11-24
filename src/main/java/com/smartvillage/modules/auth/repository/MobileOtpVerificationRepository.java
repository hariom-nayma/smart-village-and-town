package com.smartvillage.modules.auth.repository;

import com.smartvillage.modules.auth.entity.MobileOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MobileOtpVerificationRepository extends JpaRepository<MobileOtpVerification, UUID> {

    Optional<MobileOtpVerification> findTopByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(String phone, String purpose);

    Optional<MobileOtpVerification> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);

    List<MobileOtpVerification> findByPhoneAndVerifiedFalse(String phone);

    long countByPhoneAndCreatedAtAfter(String phone, Instant after);
}
