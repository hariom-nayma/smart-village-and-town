package com.smartvillage.modules.civic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateDevelopmentProjectRequest {
    @NotBlank(message = "Project name is required")
    private String projectName;

    private String village;
    private String ward;

    @NotBlank(message = "Category is required")
    private String category;

    private String responsibleDepartment;
    private BigDecimal approvedCost;
    private LocalDate startDate;
    private LocalDate expectedCompletionDate;
    private String status = "planned"; // planned, in_progress, delayed, completed
    private String milestones;
    private String photos;
    private String completionReport;
}
