package com.smartvillage.modules.delivery.repository;

import com.smartvillage.modules.delivery.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {
    Optional<DeliveryAssignment> findByOrderId(UUID orderId);
    List<DeliveryAssignment> findByDeliveryAgentIdOrderByAssignedAtDesc(UUID deliveryAgentId);
}
