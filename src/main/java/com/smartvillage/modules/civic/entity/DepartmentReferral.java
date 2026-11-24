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
@Table(name = "constituency_department_referrals", indexes = {
        @Index(name = "idx_dept_referrals_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_dept_referrals_complaint", columnList = "complaint_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentReferral extends TenantEntity {

    @Column(name = "complaint_id", nullable = false)
    private UUID complaintId;

    @Column(name = "department_name", nullable = false, length = 150)
    private String departmentName;

    @Column(name = "official_reference_number", length = 100)
    private String officialReferenceNumber;

    @Column(name = "date_of_referral", nullable = false)
    private LocalDate dateOfReferral;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "supporting_documents", columnDefinition = "TEXT")
    private String supportingDocuments;

    @Column(name = "response_received", columnDefinition = "TEXT")
    private String responseReceived;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "referred"; // referred, work_initiated, verified_resolution, rejected

    @Column(name = "action_outcome", columnDefinition = "TEXT")
    private String actionOutcome;

    @Column(name = "created_by")
    private UUID createdBy;
}
