package com.smartvillage.modules.marketplace.repository;

import com.smartvillage.modules.marketplace.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID tenantId);
    List<Category> findByTenantIdOrderBySortOrderAscNameAsc(UUID tenantId);
    Optional<Category> findByTenantIdAndSlug(UUID tenantId, String slug);
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);
}
