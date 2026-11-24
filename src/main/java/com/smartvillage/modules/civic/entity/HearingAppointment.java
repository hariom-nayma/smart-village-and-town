package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "constituency_hearing_appointments", indexes = {
        @Index(name = "idx_hearing_appointments_tenant", columnList = "tenant_id, status"),
        @Index(name = "idx_hearing_appointments_phone", columnList = "citizen_phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HearingAppointment extends TenantEntity {

    @Column(name = "hearing_id")
    private UUID hearingId;

    @Column(name = "citizen_name", nullable = false, length = 150)
    private String citizenName;

    @Column(name = "citizen_phone", nullable = false, length = 20)
    private String citizenPhone;

    @Column(length = 120)
    private String village;

    @Column(name = "issue_category", nullable = false, length = 100)
    private String issueCategory;

    @Column(name = "issue_description", nullable = false, columnDefinition = "TEXT")
    private String issueDescription;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Column(name = "slot_number")
    private Integer slotNumber;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "requested"; // requested, confirmed, rescheduled, cancelled, attended, no_show

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "follow_up_complaint_id")
    private UUID followUpComplaintId;
}
