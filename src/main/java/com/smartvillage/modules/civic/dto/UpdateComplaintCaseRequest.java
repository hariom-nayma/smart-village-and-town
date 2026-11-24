package com.smartvillage.modules.civic.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UpdateComplaintCaseRequest {
    private String status;
    private String priority;
    private String department;
    private UUID assignedTo;
    private String village;
    private String ward;
    private Instant followUpAt;
    private UUID duplicateOf;
    private String resolutionNote;
    private String publicUpdateNote;
}
