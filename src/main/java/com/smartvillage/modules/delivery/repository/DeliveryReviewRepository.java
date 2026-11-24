package com.smartvillage.modules.delivery.repository;

import com.smartvillage.modules.delivery.entity.DeliveryReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryReviewRepository extends JpaRepository<DeliveryReview, UUID> {
    List<DeliveryReview> findByDeliveryAgentIdOrderByCreatedAtDesc(UUID deliveryAgentId);
    Optional<DeliveryReview> findByOrderId(UUID orderId);
    List<DeliveryReview> findByCustomerAuthIdOrderByCreatedAtDesc(UUID customerAuthId);
}
