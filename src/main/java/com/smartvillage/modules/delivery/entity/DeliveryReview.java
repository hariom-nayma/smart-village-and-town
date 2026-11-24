package com.smartvillage.modules.delivery.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "marketplace_delivery_reviews", indexes = {
        @Index(name = "idx_mp_reviews_agent", columnList = "delivery_agent_id, created_at"),
        @Index(name = "idx_mp_reviews_order", columnList = "order_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryReview extends TenantEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "delivery_agent_id", nullable = false)
    private UUID deliveryAgentId;

    @Column(name = "customer_auth_id", nullable = false)
    private UUID customerAuthId;

    @Column(nullable = false)
    private int rating; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String review;
}
