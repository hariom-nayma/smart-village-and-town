package com.smartvillage.modules.delivery.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "marketplace_delivery_agents", indexes = {
        @Index(name = "idx_mp_agents_tenant", columnList = "tenant_id, is_active, availability_status"),
        @Index(name = "idx_mp_agents_auth", columnList = "auth_user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAgent extends TenantEntity {

    @Column(name = "auth_user_id", nullable = false, unique = true)
    private UUID authUserId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "vehicle_type", length = 60)
    private String vehicleType;

    @Column(name = "vehicle_number", length = 60)
    private String vehicleNumber;

    @Column(name = "profile_photo_path")
    private String profilePhotoPath;

    @Column(name = "vehicle_photo_path")
    private String vehiclePhotoPath;

    @Column(name = "aadhaar_photo_path")
    private String aadhaarPhotoPath;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "availability_status", nullable = false, length = 30)
    private String availabilityStatus = "offline"; // offline, available, busy
}
