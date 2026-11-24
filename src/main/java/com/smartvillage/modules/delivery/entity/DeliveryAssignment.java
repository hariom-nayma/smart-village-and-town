package com.smartvillage.modules.delivery.entity;

import com.smartvillage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_delivery_assignments", indexes = {
        @Index(name = "idx_mp_assignments_agent", columnList = "delivery_agent_id, status, assigned_at"),
        @Index(name = "idx_mp_assignments_order", columnList = "order_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignment extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "delivery_agent_id", nullable = false)
    private UUID deliveryAgentId;

    @Builder.Default
    @Column(nullable = false, length = 40)
    private String status = "assigned"; // assigned, accepted, picked_up, out_for_delivery, delivered, cancelled

    @Builder.Default
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivery_note", columnDefinition = "TEXT")
    private String deliveryNote;
}
