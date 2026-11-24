package com.smartvillage.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(UUID tenantId, UUID actorId, String action, String entityType, String entityId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actorId(actorId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();
            auditLog.setTenantId(tenantId);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit log: action={}, entityType={}, entityId={}", action, entityType, entityId, e);
        }
    }
}
