package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.ComplaintFieldVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintFieldVisitRepository extends JpaRepository<ComplaintFieldVisit, UUID> {
    List<ComplaintFieldVisit> findByComplaintIdOrderByScheduledAtDesc(UUID complaintId);
    Page<ComplaintFieldVisit> findByTenantIdOrderByScheduledAtDesc(UUID tenantId, Pageable pageable);
    List<ComplaintFieldVisit> findByTenantIdOrderByScheduledAtDesc(UUID tenantId);
    List<ComplaintFieldVisit> findByTenantIdInOrderByScheduledAtDesc(java.util.Collection<UUID> tenantIds);
    long countByTenantId(UUID tenantId);
    long countByTenantIdIn(java.util.Collection<UUID> tenantIds);
    long countByTenantIdAndCompletedAtIsNotNull(UUID tenantId);
    long countByTenantIdInAndCompletedAtIsNotNull(java.util.Collection<UUID> tenantIds);
}
