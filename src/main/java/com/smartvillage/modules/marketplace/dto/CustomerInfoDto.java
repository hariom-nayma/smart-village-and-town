package com.smartvillage.modules.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerInfoDto {
    @NotBlank(message = "Customer name is required")
    private String fullName;

    @NotBlank(message = "10-digit mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String mobile;

    private String addressLine1;
    private String addressLine2;
    private String area;
    private String city = "Gursarai";
    private String pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationLabel;

    private String paymentMethod = "upi"; // upi, cod, pay_at_pickup
}
