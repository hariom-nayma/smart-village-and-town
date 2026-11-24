package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.DepartmentReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentReferralRepository extends JpaRepository<DepartmentReferral, UUID> {
    List<DepartmentReferral> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<DepartmentReferral> findByComplaintIdOrderByCreatedAtDesc(UUID complaintId);
    long countByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantId(UUID tenantId);
}
