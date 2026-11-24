package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "complaint_updates", indexes = {
        @Index(name = "idx_complaint_updates_complaint", columnList = "complaint_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintUpdate extends TenantEntity {

    @Column(name = "complaint_id", nullable = false)
    private UUID complaintId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by")
    private UUID createdBy;
}
