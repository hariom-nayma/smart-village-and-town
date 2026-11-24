package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaint_field_visits", indexes = {
        @Index(name = "idx_field_visits_complaint", columnList = "complaint_id"),
        @Index(name = "idx_field_visits_tenant_sched", columnList = "tenant_id, scheduled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintFieldVisit extends TenantEntity {

    @Column(name = "complaint_id", nullable = false)
    private UUID complaintId;

    @Column(length = 120)
    private String village;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(nullable = false, length = 1000)
    private String purpose;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @Column(name = "pending_action", columnDefinition = "TEXT")
    private String pendingAction;

    @Column(name = "before_photos", columnDefinition = "TEXT")
    private String beforePhotos; // JSON array of photo URLs

    @Column(name = "after_photos", columnDefinition = "TEXT")
    private String afterPhotos; // JSON array of photo URLs

    @Column(name = "next_follow_up_at")
    private Instant nextFollowUpAt;

    @Column(length = 50)
    private String outcome; // scheduled, completed, follow_up_needed, unable_to_access

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
