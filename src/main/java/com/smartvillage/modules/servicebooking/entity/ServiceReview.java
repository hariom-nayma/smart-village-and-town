package com.smartvillage.modules.servicebooking.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "marketplace_service_reviews", indexes = {
        @Index(name = "idx_mp_srv_reviews_booking", columnList = "booking_id", unique = true),
        @Index(name = "idx_mp_srv_reviews_vendor", columnList = "vendor_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceReview extends TenantEntity {

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "customer_auth_id", nullable = false)
    private UUID customerAuthId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(nullable = false)
    private int rating; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String review;
}
