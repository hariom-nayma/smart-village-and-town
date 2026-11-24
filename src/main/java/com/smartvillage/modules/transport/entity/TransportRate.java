package com.smartvillage.modules.transport.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "marketplace_transport_rates", indexes = {
        @Index(name = "idx_mp_transport_rates_vehicle", columnList = "vehicle_id, service_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportRate extends TenantEntity {

    @Column(name = "delivery_agent_id", nullable = false)
    private UUID deliveryAgentId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType; // passenger, goods, parcel, luggage

    @Column(name = "rate_per_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKm;

    @Builder.Default
    @Column(name = "minimum_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumFare = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "waiting_rate_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal waitingRatePerHour = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
