package com.smartvillage.modules.notification.repository;

import com.smartvillage.modules.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientAuthIdOrderByCreatedAtDesc(UUID recipientAuthId);
    List<Notification> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    void deleteByCampaignIdAndTenantId(UUID campaignId, UUID tenantId);
}
