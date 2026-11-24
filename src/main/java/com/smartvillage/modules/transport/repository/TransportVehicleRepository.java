package com.smartvillage.modules.transport.repository;

import com.smartvillage.modules.transport.entity.TransportVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransportVehicleRepository extends JpaRepository<TransportVehicle, UUID> {
    List<TransportVehicle> findByTenantIdAndApprovalStatusAndIsActiveTrue(UUID tenantId, String approvalStatus);
    List<TransportVehicle> findByDeliveryAgentId(UUID deliveryAgentId);
    List<TransportVehicle> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<TransportVehicle> findByTenantIdAndRegistrationNumber(UUID tenantId, String registrationNumber);
}
