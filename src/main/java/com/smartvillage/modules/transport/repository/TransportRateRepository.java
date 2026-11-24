package com.smartvillage.modules.transport.repository;

import com.smartvillage.modules.transport.entity.TransportRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransportRateRepository extends JpaRepository<TransportRate, UUID> {
    List<TransportRate> findByVehicleId(UUID vehicleId);
    List<TransportRate> findByDeliveryAgentId(UUID deliveryAgentId);
    Optional<TransportRate> findByVehicleIdAndServiceType(UUID vehicleId, String serviceType);
    List<TransportRate> findByTenantIdAndServiceTypeAndIsActiveTrue(UUID tenantId, String serviceType);
}
