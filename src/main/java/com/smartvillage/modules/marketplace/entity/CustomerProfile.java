package com.smartvillage.modules.marketplace.entity;

import com.smartvillage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_customer_profiles", indexes = {
        @Index(name = "idx_mp_customer_phone_tenant", columnList = "phone, tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile extends BaseEntity {

    @Column(name = "auth_user_id", nullable = false, unique = true)
    private UUID authUserId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "default_delivery_address", columnDefinition = "TEXT")
    private String defaultDeliveryAddress; // JSON string

    @Column(name = "profile_photo_path")
    private String profilePhotoPath;

    @Builder.Default
    @Column(name = "approval_status", nullable = false, length = 30)
    private String approvalStatus = "pending"; // pending, approved, rejected

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
