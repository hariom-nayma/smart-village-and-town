package com.smartvillage.modules.notification.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_notifications", indexes = {
        @Index(name = "idx_mp_notifications_recipient", columnList = "recipient_auth_id, created_at"),
        @Index(name = "idx_mp_notifications_campaign", columnList = "campaign_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends TenantEntity {

    @Column(name = "recipient_auth_id", nullable = false)
    private UUID recipientAuthId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "recipient_type", length = 30)
    private String recipientType; // customer, vendor, delivery, admin

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "read_at")
    private Instant readAt;
}
