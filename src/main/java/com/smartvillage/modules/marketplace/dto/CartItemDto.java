package com.smartvillage.modules.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CartItemDto {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Vendor ID is required")
    private UUID vendorId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
}
