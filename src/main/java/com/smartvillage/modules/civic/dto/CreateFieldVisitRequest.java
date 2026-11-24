package com.smartvillage.modules.civic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class CreateFieldVisitRequest {
    @NotNull(message = "Complaint ID is required")
    private UUID complaintId;

    private List<UUID> relatedComplaintIds; // Batch inspections

    @NotNull(message = "Schedule date/time is required")
    private Instant scheduledAt;

    private UUID assignedTo;
    private String village;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotBlank(message = "Visit purpose is required")
    private String purpose;

    private String internalNotes;
    private String actionTaken;
    private String pendingAction;
    private String beforePhotos;
    private String afterPhotos;
    private Instant nextFollowUpAt;
    private String outcome;
}
