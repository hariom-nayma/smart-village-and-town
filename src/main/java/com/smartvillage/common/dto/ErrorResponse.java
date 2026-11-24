package com.smartvillage.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Builder.Default
    private boolean success = false;
    private String error;
    private String code;
    private Integer status;
    private String path;
    private Map<String, String> validationErrors;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
