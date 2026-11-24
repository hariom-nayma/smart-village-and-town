package com.smartvillage.modules.marketplace.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_vendors", indexes = {
        @Index(name = "idx_mp_vendors_tenant_status", columnList = "tenant_id, status, is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor extends TenantEntity {

    @Column(name = "business_name", nullable = false, length = 180)
    private String businessName;

    @Column(name = "business_name_hi", length = 180)
    private String businessNameHi;

    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "pending"; // pending, approved, suspended, rejected

    @Builder.Default
    @Column(name = "delivery_available", nullable = false)
    private boolean deliveryAvailable = true;

    @Builder.Default
    @Column(name = "pickup_available", nullable = false)
    private boolean pickupAvailable = true;

    @Builder.Default
    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "delivery_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "upi_qr_url")
    private String upiQrUrl;

    @Column(name = "location_latitude", precision = 10, scale = 7)
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude", precision = 10, scale = 7)
    private BigDecimal locationLongitude;

    @Column(name = "location_label", length = 250)
    private String locationLabel;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "created_by")
    private UUID createdBy;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
