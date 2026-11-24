package com.smartvillage.modules.delivery.repository;

import com.smartvillage.modules.delivery.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {
    Optional<DeliveryAgent> findByAuthUserId(UUID authUserId);
    List<DeliveryAgent> findByTenantIdAndIsActiveTrue(UUID tenantId);
    List<DeliveryAgent> findByTenantIdOrderByFullNameAsc(UUID tenantId);
}
