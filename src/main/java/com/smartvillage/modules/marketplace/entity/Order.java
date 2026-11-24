package com.smartvillage.modules.marketplace.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "marketplace_orders", indexes = {
        @Index(name = "idx_mp_orders_number", columnList = "order_number", unique = true),
        @Index(name = "idx_mp_orders_vendor", columnList = "vendor_id, created_at"),
        @Index(name = "idx_mp_orders_customer", columnList = "customer_auth_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends TenantEntity {

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "customer_auth_id", nullable = false)
    private UUID customerAuthId;

    @Column(name = "order_number", nullable = false, unique = true, length = 60)
    private String orderNumber;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "delivery_address", columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress; // JSON string

    @Builder.Default
    @Column(name = "delivery_method", nullable = false, length = 30)
    private String deliveryMethod = "delivery"; // delivery, pickup

    @Builder.Default
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod = "upi"; // upi, cod, pay_at_pickup

    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = "pending"; // pending, paid, failed, refunded

    @Builder.Default
    @Column(name = "payment_review_status", nullable = false, length = 30)
    private String paymentReviewStatus = "not_required"; // not_required, pending, submitted, verified, rejected

    @Builder.Default
    @Column(name = "order_status", nullable = false, length = 30)
    private String orderStatus = "placed"; // placed, accepted, processing, ready, out_for_delivery, delivered, rejected, cancelled

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "delivery_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "prepaid_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal prepaidDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "delivery_agent_id")
    private UUID deliveryAgentId;

    @Column(name = "delivery_otp_hash", length = 100)
    private String deliveryOtpHash;

    @Column(name = "delivery_otp_sent_at")
    private Instant deliveryOtpSentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "payment_collected_at")
    private Instant paymentCollectedAt;

    @Column(name = "payment_collected_by")
    private UUID paymentCollectedBy;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
