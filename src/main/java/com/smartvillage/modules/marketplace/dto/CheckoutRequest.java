package com.smartvillage.modules.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    private String tenant;

    @NotNull(message = "Customer details are required")
    @Valid
    private CustomerInfoDto customer;

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<CartItemDto> items;

    private String fulfillmentType = "delivery"; // delivery, pickup
    private String notes;
}
