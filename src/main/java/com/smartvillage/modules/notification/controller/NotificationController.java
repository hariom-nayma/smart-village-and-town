package com.smartvillage.modules.notification.controller;

import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.notification.entity.Notification;
import com.smartvillage.modules.notification.service.NotificationService;
import com.smartvillage.modules.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "In-App Notifications", description = "Targeted campaigns, customer notices, and read receipt tracking")
public class NotificationController {

    private final NotificationService notificationService;
    private final TenantService tenantService;

    private UUID resolveTenantId(String tenant, UUID tenantId) {
        if (tenantId != null) return tenantId;
        String slug = tenant != null ? tenant : TenantContext.getTenantSlug();
        return tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai").getId();
    }

    @GetMapping("/account/notifications")
    @Operation(summary = "Get in-app notifications for authenticated user")
    public ApiResponse<List<Notification>> getMyNotifications() {
        return ApiResponse.ok(notificationService.getMyNotifications());
    }

    @PatchMapping({"/account/notifications/{id}", "/account/notifications/{id}/read"})
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok("Notification marked as read", null);
    }

    @PostMapping("/admin/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Broadcast / send targeted notification campaign to customers, vendors, drivers, staff")
    public ApiResponse<Map<String, Object>> sendCampaign(@RequestParam(value = "tenant", required = false) String tenant,
                                                         @RequestParam(value = "tenantId", required = false) UUID tenantId,
                                                         @RequestBody Map<String, Object> body) {
        UUID effectiveTenantId = resolveTenantId(tenant, tenantId);
        List<String> types = (List<String>) body.get("types");
        List<UUID> recipientIds = null;
        if (body.get("recipient_ids") instanceof List<?> list) {
            recipientIds = list.stream().map(o -> UUID.fromString(o.toString())).toList();
        }
        String title = (String) body.get("title");
        String message = (String) body.get("message");

        return ApiResponse.ok("Notification campaign sent",
                notificationService.sendNotificationCampaign(effectiveTenantId, types, recipientIds, title, message));
    }

    @GetMapping("/admin/notifications/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get notification history for a tenant")
    public ApiResponse<List<Notification>> getHistory(@RequestParam(value = "tenant", required = false) String tenant,
                                                      @RequestParam(value = "tenantId", required = false) UUID tenantId) {
        UUID effectiveTenantId = resolveTenantId(tenant, tenantId);
        return ApiResponse.ok(notificationService.getHistory(effectiveTenantId));
    }

    @DeleteMapping("/admin/notifications/{campaignId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete an entire notification campaign")
    public ApiResponse<Void> deleteCampaign(@PathVariable UUID campaignId,
                                            @RequestParam(value = "tenant", required = false) String tenant,
                                            @RequestParam(value = "tenantId", required = false) UUID tenantId) {
        UUID effectiveTenantId = resolveTenantId(tenant, tenantId);
        notificationService.deleteCampaign(campaignId, effectiveTenantId);
        return ApiResponse.ok("Campaign deleted", null);
    }
}
