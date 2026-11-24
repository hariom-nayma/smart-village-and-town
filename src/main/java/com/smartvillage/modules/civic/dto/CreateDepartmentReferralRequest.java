package com.smartvillage.modules.civic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateDepartmentReferralRequest {
    @NotNull(message = "Complaint ID is required")
    private UUID complaintId;

    @NotBlank(message = "Department name is required")
    private String departmentName; // PWD, Electricity, Jal Nigam, Nagar Panchayat, Police, Revenue

    private String officialReferenceNumber;

    @NotNull(message = "Date of referral is required")
    private LocalDate dateOfReferral;

    private LocalDate followUpDate;
    private String supportingDocuments;
    private String responseReceived;
    private String status = "referred";
    private String actionOutcome;
}
