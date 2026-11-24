package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_tracking", columnList = "tracking_id", unique = true),
        @Index(name = "idx_complaints_tenant_status", columnList = "tenant_id, status, created_at"),
        @Index(name = "idx_complaints_assigned", columnList = "assigned_to"),
        @Index(name = "idx_complaints_village_cat", columnList = "tenant_id, village, category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint extends TenantEntity {

    @Column(name = "tracking_id", nullable = false, unique = true, length = 40)
    private String trackingId;

    @Column(name = "citizen_name", nullable = false, length = 150)
    private String citizenName;

    @Column(name = "citizen_phone", nullable = false, length = 20)
    private String citizenPhone;

    @Column(name = "citizen_email", length = 150)
    private String citizenEmail;

    @Builder.Default
    @Column(name = "is_anonymous")
    private boolean isAnonymous = false;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "ward_code", length = 50)
    private String wardCode;

    @Column(length = 120)
    private String ward;

    @Column(length = 120)
    private String village;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String priority = "normal"; // low, normal, high, urgent

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "submitted"; // submitted, under_review, in_progress, resolved, rejected

    @Column(length = 100)
    private String department;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "follow_up_at")
    private Instant followUpAt;

    @Builder.Default
    @Column(name = "is_overdue")
    private boolean isOverdue = false;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "duplicate_of")
    private UUID duplicateOf;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "citizen_feedback_rating")
    private Integer citizenFeedbackRating; // 1-5 stars

    @Column(name = "citizen_feedback_notes", columnDefinition = "TEXT")
    private String citizenFeedbackNotes;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
}
