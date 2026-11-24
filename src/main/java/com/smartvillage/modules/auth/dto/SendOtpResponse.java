package com.smartvillage.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpResponse {
    private String mobile;
    private String message;
    private int cooldownSeconds;
    private int expiryMinutes;
    private String debugOtp; // Included for developer testing / automated integration suites
}
