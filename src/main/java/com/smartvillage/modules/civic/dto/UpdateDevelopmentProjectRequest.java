package com.smartvillage.modules.civic.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateDevelopmentProjectRequest {
    private String projectName;
    private String village;
    private String ward;
    private String category;
    private String responsibleDepartment;
    private BigDecimal approvedCost;
    private LocalDate startDate;
    private LocalDate expectedCompletionDate;
    private String status;
    private String milestones;
    private String photos;
    private String completionReport;
}
