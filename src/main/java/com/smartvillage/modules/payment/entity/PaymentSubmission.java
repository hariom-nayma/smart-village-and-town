package com.smartvillage.modules.payment.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_payment_submissions", indexes = {
        @Index(name = "idx_mp_payments_vendor", columnList = "vendor_id, status, submitted_at"),
        @Index(name = "idx_mp_payments_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSubmission extends TenantEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "customer_auth_id")
    private UUID customerAuthId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "utr_number", length = 30)
    private String utrNumber;

    @Column(name = "proof_path")
    private String proofPath;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "submitted"; // submitted, verified, rejected

    @Builder.Default
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;
}
