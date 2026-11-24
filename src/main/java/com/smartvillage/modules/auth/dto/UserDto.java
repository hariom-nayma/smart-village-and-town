package com.smartvillage.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private UUID tenantId;
    private UUID vendorId;
    private boolean active;
    private Instant createdAt;
}
