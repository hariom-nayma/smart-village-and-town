package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "volunteers", indexes = {
        @Index(name = "idx_volunteers_tenant", columnList = "tenant_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Volunteer extends TenantEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 150)
    private String area;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(length = 100)
    private String availability;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "new"; // new, active, inactive
}
