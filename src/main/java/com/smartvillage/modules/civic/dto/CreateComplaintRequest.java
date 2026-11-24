package com.smartvillage.modules.civic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateComplaintRequest {
    @NotBlank(message = "Citizen name is required")
    private String citizenName;

    @NotBlank(message = "Citizen mobile number is required")
    private String citizenPhone;

    private String citizenEmail;
    private boolean isAnonymous;

    @NotBlank(message = "Category is required")
    private String category;

    private String wardCode;
    private String ward;
    private String village;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String priority = "normal"; // low, normal, high, urgent
    private String photoUrl;

    @NotBlank(message = "Complaint description is required")
    private String description;

    private UUID tenantId;
    private String tenantSlug;
}
