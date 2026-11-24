package com.smartvillage.modules.transport.repository;

import com.smartvillage.modules.transport.entity.TransportBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransportBookingRepository extends JpaRepository<TransportBooking, UUID> {
    Optional<TransportBooking> findByBookingNumber(String bookingNumber);
    List<TransportBooking> findByDeliveryAgentIdOrderByCreatedAtDesc(UUID deliveryAgentId);
    List<TransportBooking> findByCustomerAuthUserIdOrderByCreatedAtDesc(UUID customerAuthUserId);
    List<TransportBooking> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
