package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "complaint_field_visit_items", indexes = {
        @Index(name = "idx_visit_items_visit", columnList = "field_visit_id"),
        @Index(name = "idx_visit_items_complaint", columnList = "complaint_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintFieldVisitItem extends BaseEntity {

    @Column(name = "field_visit_id", nullable = false)
    private UUID fieldVisitId;

    @Column(name = "complaint_id", nullable = false)
    private UUID complaintId;

    @Column(columnDefinition = "TEXT")
    private String observations;
}
