package com.smartvillage.modules.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResultDto {
    private boolean success;
    private boolean approvalRequired;
    private String customerStatus; // pending, approved
    private List<OrderDto> orders;
}
