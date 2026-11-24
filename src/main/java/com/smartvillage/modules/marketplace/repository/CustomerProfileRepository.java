package com.smartvillage.modules.marketplace.repository;

import com.smartvillage.modules.marketplace.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
    Optional<CustomerProfile> findByAuthUserId(UUID authUserId);
    Optional<CustomerProfile> findByTenantIdAndPhone(UUID tenantId, String phone);
    List<CustomerProfile> findByTenantIdAndApprovalStatusOrderByCreatedAtAsc(UUID tenantId, String approvalStatus);
    List<CustomerProfile> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
