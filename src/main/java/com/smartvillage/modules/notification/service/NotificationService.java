package com.smartvillage.modules.notification.service;

import com.smartvillage.common.audit.AuditService;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.modules.auth.entity.User;
import com.smartvillage.modules.auth.repository.UserRepository;
import com.smartvillage.modules.notification.entity.Notification;
import com.smartvillage.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Map<String, Object> sendNotificationCampaign(UUID tenantId, List<String> types, List<UUID> specificUserIds, String title, String message) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        SecurityUtils.validateTenantAccess(tenantId);

        if (title == null || title.isBlank() || message == null || message.isBlank()) {
            throw new BadRequestException("Title and message are required.");
        }

        if (types == null || types.isEmpty()) {
            throw new BadRequestException("Select at least one recipient type.");
        }

        List<User> targetUsers = new ArrayList<>();
        for (String type : types) {
            targetUsers.addAll(userRepository.findByTenantIdAndRole(tenantId, type.toUpperCase()));
        }

        if (specificUserIds != null && !specificUserIds.isEmpty()) {
            Set<UUID> idSet = new HashSet<>(specificUserIds);
            targetUsers = targetUsers.stream().filter(u -> idSet.contains(u.getId())).toList();
        }

        UUID campaignId = UUID.randomUUID();
        List<Notification> notifications = new ArrayList<>();

        for (User target : targetUsers) {
            Notification n = Notification.builder()
                    .recipientAuthId(target.getId())
                    .title(title.trim())
                    .message(message.trim())
                    .campaignId(campaignId)
                    .recipientType(target.getRole().toLowerCase())
                    .createdBy(user.getId())
                    .build();
            n.setTenantId(tenantId);
            notifications.add(n);
        }

        notificationRepository.saveAll(notifications);

        auditService.log(tenantId, user.getId(), "SEND_NOTIFICATION_CAMPAIGN",
                "marketplace_notifications", campaignId.toString(), "Sent to " + notifications.size() + " recipients");

        Map<String, Object> res = new HashMap<>();
        res.put("campaignId", campaignId);
        res.put("sentCount", notifications.size());
        return res;
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.findByRecipientAuthIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRecipientAuthId().equals(userId)) {
            throw new ForbiddenException("Cannot mark other users' notifications as read.");
        }

        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getHistory(UUID tenantId) {
        SecurityUtils.validateTenantAccess(tenantId);
        return notificationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public void deleteCampaign(UUID campaignId, UUID tenantId) {
        SecurityUtils.validateTenantAccess(tenantId);
        notificationRepository.deleteByCampaignIdAndTenantId(campaignId, tenantId);
    }
}
