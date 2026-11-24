package com.smartvillage.modules.civic.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcements_tenant", columnList = "tenant_id, published")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement extends TenantEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 80)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean published = true;
}
