package com.smartvillage.modules.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private UUID id;
    private UUID tenantId;
    private UUID vendorId;
    private String vendorName;
    private String vendorUpiId;
    private String vendorQrUrl;
    private String upiIntentUrl;
    private boolean paymentRequired;

    private String orderNumber;
    private String customerName;
    private String customerPhone;
    private String deliveryMethod;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentReviewStatus;
    private String orderStatus;

    private BigDecimal subtotal;
    private BigDecimal deliveryCharge;
    private BigDecimal prepaidDiscount;
    private BigDecimal total;
    private String notes;

    private Instant createdAt;
    private List<OrderItemDto> items;
}
