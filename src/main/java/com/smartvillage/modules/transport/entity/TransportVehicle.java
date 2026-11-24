package com.smartvillage.modules.transport.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_transport_vehicles", indexes = {
        @Index(name = "idx_mp_transport_vehicles_driver", columnList = "delivery_agent_id"),
        @Index(name = "idx_mp_transport_vehicles_tenant", columnList = "tenant_id, approval_status, is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportVehicle extends TenantEntity {

    @Column(name = "delivery_agent_id", nullable = false)
    private UUID deliveryAgentId;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType; // e_rickshaw, auto_rickshaw, taxi, hatchback, sedan, suv, pickup, mini_truck, truck, two_wheeler

    @Column(name = "make_model", nullable = false, length = 100)
    private String makeModel;

    @Column(name = "registration_number", nullable = false, length = 50)
    private String registrationNumber;

    @Builder.Default
    @Column(name = "passenger_capacity", nullable = false)
    private int passengerCapacity = 0;

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

    @Builder.Default
    @Column(name = "supports_passengers", nullable = false)
    private boolean supportsPassengers = true;

    @Builder.Default
    @Column(name = "supports_goods", nullable = false)
    private boolean supportsGoods = false;

    @Column(name = "vehicle_photo_path")
    private String vehiclePhotoPath;

    @Column(name = "rc_document_path")
    private String rcDocumentPath;

    @Column(name = "driving_licence_path")
    private String drivingLicencePath;

    @Column(name = "insurance_document_path")
    private String insuranceDocumentPath;

    @Column(name = "permit_document_path")
    private String permitDocumentPath;

    @Builder.Default
    @Column(name = "approval_status", nullable = false, length = 40)
    private String approvalStatus = "pending"; // pending, approved, rejected, suspended, changes_requested

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
