package com.smartvillage.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpLoginRequest {

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "OTP code is required")
    private String otp;

    private String tenant;
    private String fullName; // For new customer auto-registration
}
