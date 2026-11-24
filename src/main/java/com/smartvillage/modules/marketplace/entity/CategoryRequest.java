package com.smartvillage.modules.marketplace.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_category_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest extends TenantEntity {

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "requested_name", nullable = false, length = 100)
    private String requestedName;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "pending"; // pending, approved, rejected

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;
}
