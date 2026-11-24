package com.smartvillage.modules.civic.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateDepartmentReferralRequest {
    private String officialReferenceNumber;
    private LocalDate followUpDate;
    private String responseReceived;
    private String status; // referred, work_initiated, verified_resolution, rejected
    private String actionOutcome;
}
