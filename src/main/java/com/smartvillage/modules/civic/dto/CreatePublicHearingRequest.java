package com.smartvillage.modules.civic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePublicHearingRequest {
    @NotNull(message = "Hearing date is required")
    private LocalDate hearingDate;

    @NotBlank(message = "Hearing title is required")
    private String hearingTitle;

    @NotBlank(message = "Location is required")
    private String location;

    private int maxSlots = 50;
    private String notes;
}
