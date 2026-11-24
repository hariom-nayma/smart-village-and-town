package com.smartvillage.modules.servicebooking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateServiceBookingRequest {
    @NotNull(message = "Service product ID is required")
    private UUID productId;

    private String tenant;
    private Instant requestedFor;
    private boolean atCustomerAddress = true;

    private String addressLine1;
    private String area;
    private String city;
    private String notes;
}
