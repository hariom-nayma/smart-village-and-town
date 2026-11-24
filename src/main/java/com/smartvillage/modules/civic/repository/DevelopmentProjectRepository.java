package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.DevelopmentProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DevelopmentProjectRepository extends JpaRepository<DevelopmentProject, UUID> {
    List<DevelopmentProject> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Page<DevelopmentProject> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    long countByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantId(UUID tenantId);
}
