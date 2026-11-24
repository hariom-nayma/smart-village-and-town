package com.smartvillage.modules.servicebooking.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_service_bookings", indexes = {
        @Index(name = "idx_mp_service_bookings_number", columnList = "booking_number", unique = true),
        @Index(name = "idx_mp_service_bookings_vendor", columnList = "vendor_id, booking_status, created_at"),
        @Index(name = "idx_mp_service_bookings_customer", columnList = "customer_auth_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceBooking extends TenantEntity {

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "customer_auth_id", nullable = false)
    private UUID customerAuthId;

    @Column(name = "booking_number", nullable = false, unique = true, length = 60)
    private String bookingNumber;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "requested_for")
    private Instant requestedFor;

    @Column(name = "service_address", columnDefinition = "TEXT", nullable = false)
    private String serviceAddress; // JSON string

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "estimated_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "booking_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookingCharge = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "visiting_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal visitingCharge = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "upfront_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal upfrontAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "remaining_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = "pending"; // pending, paid, not_required, rejected

    @Builder.Default
    @Column(name = "booking_status", nullable = false, length = 40)
    private String bookingStatus = "pending_payment"; // pending_payment, pending_vendor, accepted, in_progress, awaiting_customer_confirmation, completed, cancelled, rejected

    @Column(name = "completion_code", length = 10)
    private String completionCode;

    @Column(name = "completion_code_created_at")
    private Instant completionCodeCreatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Builder.Default
    @Column(name = "refund_status", length = 30)
    private String refundStatus = "not_applicable"; // not_applicable, requested, processing, refunded, rejected

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_requested_at")
    private Instant refundRequestedAt;

    @Column(name = "refund_processed_at")
    private Instant refundProcessedAt;

    @Column(name = "refund_note", columnDefinition = "TEXT")
    private String refundNote;

    @Builder.Default
    @Column(name = "remaining_payment_status", length = 30)
    private String remainingPaymentStatus = "not_required"; // not_required, pending, paid

    @Column(name = "remaining_paid_amount", precision = 12, scale = 2)
    private BigDecimal remainingPaidAmount;
}
