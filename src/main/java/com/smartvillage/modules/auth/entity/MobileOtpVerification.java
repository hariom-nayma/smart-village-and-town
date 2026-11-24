package com.smartvillage.modules.auth.entity;

import com.smartvillage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mobile_otp_verifications", indexes = {
        @Index(name = "idx_otp_phone", columnList = "phone"),
        @Index(name = "idx_otp_phone_purpose", columnList = "phone, purpose, verified"),
        @Index(name = "idx_otp_created_at", columnList = "phone, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileOtpVerification extends BaseEntity {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String purpose = "LOGIN";

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
