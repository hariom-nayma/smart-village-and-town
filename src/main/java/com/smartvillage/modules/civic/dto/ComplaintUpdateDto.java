package com.smartvillage.modules.civic.dto;

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
public class ComplaintUpdateDto {
    private UUID id;
    private String status;
    private String note;
    private Instant createdAt;
}
