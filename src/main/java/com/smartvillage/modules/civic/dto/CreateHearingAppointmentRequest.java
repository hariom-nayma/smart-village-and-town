package com.smartvillage.modules.civic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateHearingAppointmentRequest {
    private UUID hearingId;

    @NotBlank(message = "Citizen name is required")
    private String citizenName;

    @NotBlank(message = "Citizen mobile is required")
    private String citizenPhone;

    private String village;

    @NotBlank(message = "Issue category is required")
    private String issueCategory;

    @NotBlank(message = "Issue description is required")
    private String issueDescription;

    @NotNull(message = "Preferred date is required")
    private LocalDate preferredDate;
}
