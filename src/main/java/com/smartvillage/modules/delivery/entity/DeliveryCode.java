package com.smartvillage.modules.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_delivery_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCode {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 10)
    private String code;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
