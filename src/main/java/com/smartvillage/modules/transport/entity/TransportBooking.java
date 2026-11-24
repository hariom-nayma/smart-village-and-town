package com.smartvillage.modules.transport.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_transport_bookings", indexes = {
        @Index(name = "idx_mp_transport_bookings_number", columnList = "booking_number", unique = true),
        @Index(name = "idx_mp_transport_bookings_driver", columnList = "delivery_agent_id, booking_status"),
        @Index(name = "idx_mp_transport_bookings_customer", columnList = "customer_auth_user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportBooking extends TenantEntity {

    @Column(name = "booking_number", nullable = false, unique = true, length = 60)
    private String bookingNumber;

    @Column(name = "customer_auth_user_id", nullable = false)
    private UUID customerAuthUserId;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType; // passenger, goods, parcel, luggage

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "delivery_agent_id")
    private UUID deliveryAgentId;

    @Column(name = "rate_id")
    private UUID rateId;

    @Column(name = "source_address", nullable = false, length = 500)
    private String sourceAddress;

    @Column(name = "source_latitude", precision = 10, scale = 7)
    private BigDecimal sourceLatitude;

    @Column(name = "source_longitude", precision = 10, scale = 7)
    private BigDecimal sourceLongitude;

    @Column(name = "destination_address", nullable = false, length = 500)
    private String destinationAddress;

    @Column(name = "destination_latitude", precision = 10, scale = 7)
    private BigDecimal destinationLatitude;

    @Column(name = "destination_longitude", precision = 10, scale = 7)
    private BigDecimal destinationLongitude;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "travel_at")
    private Instant travelAt;

    @Builder.Default
    @Column(name = "passenger_count", nullable = false)
    private int passengerCount = 0;

    @Column(name = "goods_description", columnDefinition = "TEXT")
    private String goodsDescription;

    @Builder.Default
    @Column(name = "package_count", nullable = false)
    private int packageCount = 0;

    @Builder.Default
    @Column(name = "cargo_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal cargoWeightKg = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cargo_area_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal cargoAreaSqft = BigDecimal.ZERO;

    @Column(name = "cargo_length_cm", precision = 10, scale = 2)
    private BigDecimal cargoLengthCm;

    @Column(name = "cargo_width_cm", precision = 10, scale = 2)
    private BigDecimal cargoWidthCm;

    @Column(name = "cargo_height_cm", precision = 10, scale = 2)
    private BigDecimal cargoHeightCm;

    @Column(name = "handling_notes", columnDefinition = "TEXT")
    private String handlingNotes;

    @Column(name = "rate_per_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKm;

    @Builder.Default
    @Column(name = "minimum_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumFare = BigDecimal.ZERO;

    @Column(name = "estimated_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "final_fare", precision = 12, scale = 2)
    private BigDecimal finalFare;

    @Builder.Default
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod = "pay_driver"; // pay_driver, platform

    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = "pending"; // pending, paid, failed, refunded

    @Builder.Default
    @Column(name = "booking_status", nullable = false, length = 40)
    private String bookingStatus = "requested"; // requested, accepted, driver_arriving, in_progress, completed, cancelled, rejected

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
