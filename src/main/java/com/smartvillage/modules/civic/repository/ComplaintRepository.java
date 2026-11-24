package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    Optional<Complaint> findByTrackingIdAndTenantId(String trackingId, UUID tenantId);
    Page<Complaint> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    List<Complaint> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Complaint> findByTenantIdAndAssignedToOrderByCreatedAtDesc(UUID tenantId, UUID assignedTo);
    long countByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantId(UUID tenantId);

    @Query("SELECT c.village, c.category, COUNT(c) FROM Complaint c WHERE c.tenantId = :tenantId GROUP BY c.village, c.category")
    List<Object[]> getVillageCategoryMatrix(@Param("tenantId") UUID tenantId);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c WHERE c.tenantId = :tenantId GROUP BY c.category")
    List<Object[]> getCategoryCounts(@Param("tenantId") UUID tenantId);

    @Query("SELECT c.village, COUNT(c) FROM Complaint c WHERE c.tenantId = :tenantId AND lower(c.status) NOT IN ('resolved', 'rejected') GROUP BY c.village ORDER BY COUNT(c) DESC")
    List<Object[]> getPendingByVillage(@Param("tenantId") UUID tenantId);
}
