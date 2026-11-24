package com.smartvillage.modules.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class CreateTransportBookingRequest {
    private String tenant;

    @NotBlank(message = "Service type is required (passenger, goods, parcel, luggage)")
    private String serviceType;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    private UUID vehicleId;

    @NotBlank(message = "Source address is required")
    private String sourceAddress;
    private BigDecimal sourceLatitude;
    private BigDecimal sourceLongitude;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;
    private BigDecimal destinationLatitude;
    private BigDecimal destinationLongitude;

    @NotNull(message = "Distance in km is required")
    @Positive(message = "Distance must be positive")
    private BigDecimal distanceKm;

    private Instant travelAt;
    private int passengerCount = 0;
    private String goodsDescription;
    private int packageCount = 0;
    private BigDecimal cargoWeightKg = BigDecimal.ZERO;
    private BigDecimal cargoAreaSqft = BigDecimal.ZERO;
    private String customerNote;
}
