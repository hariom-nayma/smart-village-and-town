package com.smartvillage.modules.marketplace.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marketplace_categories", indexes = {
        @Index(name = "idx_mp_categories_tenant_slug", columnList = "tenant_id, slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends TenantEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_hi", length = 100)
    private String nameHi;

    @Column(nullable = false, length = 100)
    private String slug;

    @Builder.Default
    @Column(length = 20)
    private String icon = "🛍️";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
