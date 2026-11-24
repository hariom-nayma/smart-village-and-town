package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "constituency_public_hearings", indexes = {
        @Index(name = "idx_hearings_tenant_date", columnList = "tenant_id, hearing_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicHearing extends TenantEntity {

    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @Column(name = "hearing_title", nullable = false, length = 200)
    private String hearingTitle;

    @Column(nullable = false, length = 200)
    private String location;

    @Builder.Default
    @Column(name = "max_slots", nullable = false)
    private int maxSlots = 50;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "scheduled"; // scheduled, completed, cancelled

    @Column(columnDefinition = "TEXT")
    private String notes;
}
