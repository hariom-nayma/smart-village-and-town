package com.smartvillage.modules.marketplace.repository;

import com.smartvillage.modules.marketplace.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findByTenantIdAndStatusAndIsActiveTrue(UUID tenantId, String status);
    List<Vendor> findByTenantIdOrderByBusinessNameAsc(UUID tenantId);
    Optional<Vendor> findByCreatedBy(UUID userId);
    Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId);
}
