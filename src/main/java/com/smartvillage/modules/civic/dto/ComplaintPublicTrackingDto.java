package com.smartvillage.modules.civic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintPublicTrackingDto {
    private UUID id;
    private String trackingId;
    private String category;
    private String ward;
    private String village;
    private String status;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ComplaintUpdateDto> publicUpdates;
}
