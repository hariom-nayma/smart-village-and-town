package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "constituency_development_projects", indexes = {
        @Index(name = "idx_dev_projects_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_dev_projects_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevelopmentProject extends TenantEntity {

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(length = 120)
    private String village;

    @Column(length = 120)
    private String ward;

    @Column(nullable = false, length = 100)
    private String category; // Roads & Bridges, Drinking Water, Drainage, Solar & Streetlights, Health, Education

    @Column(name = "responsible_department", length = 150)
    private String responsibleDepartment;

    @Column(name = "approved_cost", precision = 14, scale = 2)
    private BigDecimal approvedCost;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "planned"; // planned, in_progress, delayed, completed

    @Column(columnDefinition = "TEXT")
    private String milestones; // JSON representation of milestones

    @Column(columnDefinition = "TEXT")
    private String photos; // JSON array of progress photos

    @Column(name = "completion_report", columnDefinition = "TEXT")
    private String completionReport;

    @Column(name = "created_by")
    private UUID createdBy;
}
