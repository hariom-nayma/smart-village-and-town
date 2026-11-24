package com.smartvillage.modules.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    private UUID id;
    private String name;
    private String slug;
    private String district;
    private String state;
    private String pincode;
    private String contactPhone;
    private String contactEmail;
    private String constituency;
    private String bodyType;
    private boolean active;
}
