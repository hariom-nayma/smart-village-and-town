package com.smartvillage.modules.marketplace.repository;

import com.smartvillage.modules.marketplace.entity.CategoryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, UUID> {
    List<CategoryRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<CategoryRequest> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
}
