package com.smartvillage.modules.civic.controller;

import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.common.dto.PagedResponse;
import com.smartvillage.modules.auth.dto.UserDto;
import com.smartvillage.modules.civic.dto.*;
import com.smartvillage.modules.civic.entity.*;
import com.smartvillage.modules.civic.service.CivicService;
import com.smartvillage.modules.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Smart Constituency & MLA Command Center", description = "Constituency Command Center, Village Heatmaps, Multi-Issue Inspections, Department Referrals, Hearings & Development Tracker")
public class CivicController {

    private final CivicService civicService;
    private final TenantService tenantService;

    // ==========================================
    // 1. PUBLIC CITIZEN INTERFACES
    // ==========================================

    @PostMapping("/public/complaints")
    @Operation(summary = "Submit a citizen complaint / case report")
    public ApiResponse<ComplaintPublicTrackingDto> submitComplaint(@Valid @RequestBody CreateComplaintRequest request) {
        return ApiResponse.ok("Complaint submitted successfully", civicService.submitComplaint(request));
    }

    @GetMapping("/public/complaints/track/{trackingId}")
    @Operation(summary = "Track complaint progress by tracking ID (privacy-safe public view)")
    public ApiResponse<ComplaintPublicTrackingDto> trackComplaint(@PathVariable String trackingId,
                                                                 @RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.trackComplaint(trackingId, tenant));
    }

    @PostMapping("/public/complaints/{trackingId}/feedback")
    @Operation(summary = "Submit citizen satisfaction rating and feedback on resolved case")
    public ApiResponse<Void> submitFeedback(@PathVariable String trackingId,
                                            @RequestParam(required = false) String tenant,
                                            @Valid @RequestBody CitizenFeedbackRequest request) {
        civicService.submitCitizenFeedback(trackingId, tenant, request);
        return ApiResponse.ok("Feedback submitted successfully. Thank you for your response.", null);
    }

    @GetMapping("/public/civic/metrics")
    @Operation(summary = "Get aggregate complaint counts (total, open, resolved)")
    public ApiResponse<Map<String, Object>> getCivicMetrics(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getCivicMetrics(tenant));
    }

    @GetMapping("/public/constituency/projects")
    @Operation(summary = "Get public-facing development projects in the constituency")
    public ApiResponse<List<DevelopmentProject>> getPublicProjects(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getDevelopmentProjects(tenant));
    }

    @GetMapping("/public/constituency/hearings")
    @Operation(summary = "Get upcoming public hearing dates and locations")
    public ApiResponse<List<PublicHearing>> getPublicHearings(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getPublicHearings(tenant));
    }

    @PostMapping("/public/constituency/hearings/appointments")
    @Operation(summary = "Citizen requests an appointment slot for a public hearing")
    public ApiResponse<HearingAppointment> requestHearingAppointment(@Valid @RequestBody CreateHearingAppointmentRequest request,
                                                                     @RequestParam(required = false) String tenant) {
        return ApiResponse.ok("Hearing appointment requested successfully", civicService.requestHearingAppointment(request, tenant));
    }

    // ==========================================
    // 2. CONSTITUENCY COMMAND CENTER & INTELLIGENCE (Staff / Admin / MLA)
    // ==========================================

    @GetMapping("/constituency/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Constituency Command Center: metrics, urgent cases, field visits, and Focus of the Day")
    public ApiResponse<Map<String, Object>> getDashboard(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getConstituencyDashboard(tenant));
    }

    @GetMapping("/constituency/heatmap")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Village-wise problem heatmap matrix, issue categories, and problem clusters")
    public ApiResponse<Map<String, Object>> getHeatmap(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getVillageProblemHeatmap(tenant));
    }

    @GetMapping("/constituency/intelligence")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Constituency Intelligence: answers the 5 core operational questions")
    public ApiResponse<Map<String, Object>> getIntelligence(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getConstituencyIntelligence(tenant));
    }

    @GetMapping("/constituency/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Monthly and quarterly constituency service accountability reports")
    public ApiResponse<Map<String, Object>> getReports(@RequestParam(required = false) String tenant,
                                                       @RequestParam(required = false) String period) {
        return ApiResponse.ok(civicService.getConstituencyReport(tenant, period));
    }

    @GetMapping("/constituency/reports/export-csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Export constituency grievance data to CSV for Excel analysis")
    public void exportCsv(@RequestParam(required = false) String tenant, HttpServletResponse response) throws IOException {
        String csv = civicService.exportConstituencyReportCsv(tenant);
        response.setContentType("text/csv; charset=utf-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"constituency-grievances-" + (tenant != null ? tenant : "report") + ".csv\"");
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }

    // ==========================================
    // 3. FIELD VISITS & MULTI-ISSUE INSPECTIONS
    // ==========================================

    @GetMapping("/constituency/field-visits")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "List all scheduled and completed field visits with covered complaints")
    public ApiResponse<List<Map<String, Object>>> getFieldVisits(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getFieldVisits(tenant));
    }

    @PostMapping({"/admin/field-visits", "/constituency/field-visits"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Schedule a constituency office field visit (with multi-complaint batching)")
    public ApiResponse<ComplaintFieldVisit> scheduleFieldVisit(@Valid @RequestBody CreateFieldVisitRequest request) {
        return ApiResponse.ok("Field visit scheduled successfully", civicService.scheduleFieldVisit(request));
    }

    @PatchMapping("/constituency/field-visits/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Complete field visit with before/after photos and action taken report")
    public ApiResponse<ComplaintFieldVisit> completeFieldVisit(@PathVariable UUID id,
                                                               @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Field visit completed", civicService.completeFieldVisit(
                id,
                body.get("outcome"),
                body.get("actionTaken"),
                body.get("pendingAction"),
                body.get("beforePhotos"),
                body.get("afterPhotos"),
                body.get("internalNotes")
        ));
    }

    // ==========================================
    // 4. DEPARTMENT REFERRAL & FOLLOW-UP TRACKER
    // ==========================================

    @GetMapping("/constituency/department-referrals")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "List all external department referrals (PWD, Jal Nigam, Electricity, Police, etc.)")
    public ApiResponse<List<DepartmentReferral>> getDepartmentReferrals(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getDepartmentReferrals(tenant));
    }

    @PostMapping("/constituency/department-referrals")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Refer a case to an external government authority with reference number")
    public ApiResponse<DepartmentReferral> createDepartmentReferral(@Valid @RequestBody CreateDepartmentReferralRequest request) {
        return ApiResponse.ok("Case referred to department successfully", civicService.createDepartmentReferral(request));
    }

    @PatchMapping("/constituency/department-referrals/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Update department referral outcome (referred, work_initiated, verified_resolution, rejected)")
    public ApiResponse<DepartmentReferral> updateDepartmentReferral(@PathVariable UUID id,
                                                                   @RequestBody UpdateDepartmentReferralRequest request) {
        return ApiResponse.ok("Department referral updated", civicService.updateDepartmentReferral(id, request));
    }

    // ==========================================
    // 5. DEVELOPMENT PROJECTS MANAGEMENT
    // ==========================================

    @PostMapping("/constituency/projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Create a development project record with approved budget and milestones")
    public ApiResponse<DevelopmentProject> createProject(@Valid @RequestBody CreateDevelopmentProjectRequest request,
                                                         @RequestParam(required = false) String tenant) {
        return ApiResponse.ok("Project created successfully", civicService.createDevelopmentProject(request, tenant));
    }

    @PatchMapping("/constituency/projects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Update development project status, milestones, photos, or completion report")
    public ApiResponse<DevelopmentProject> updateProject(@PathVariable UUID id,
                                                         @RequestBody UpdateDevelopmentProjectRequest request) {
        return ApiResponse.ok("Project updated", civicService.updateDevelopmentProject(id, request));
    }

    // ==========================================
    // 6. PUBLIC HEARINGS & APPOINTMENT MANAGEMENT
    // ==========================================

    @PostMapping("/constituency/hearings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Schedule a public hearing date, location, and slot capacity")
    public ApiResponse<PublicHearing> createHearing(@Valid @RequestBody CreatePublicHearingRequest request,
                                                    @RequestParam(required = false) String tenant) {
        return ApiResponse.ok("Public hearing scheduled", civicService.createPublicHearing(request, tenant));
    }

    @GetMapping("/constituency/hearings/appointments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "List citizen appointment requests for public hearings")
    public ApiResponse<List<HearingAppointment>> getHearingAppointments(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(civicService.getHearingAppointments(tenant));
    }

    @PatchMapping("/constituency/hearings/appointments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Update appointment status (confirmed, attended, rescheduled, cancelled) or slot number")
    public ApiResponse<HearingAppointment> updateHearingAppointment(@PathVariable UUID id,
                                                                    @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        Integer slot = body.get("slotNumber") != null ? ((Number) body.get("slotNumber")).intValue() : null;
        String notes = (String) body.get("adminNotes");
        return ApiResponse.ok("Appointment updated", civicService.updateHearingAppointment(id, status, slot, notes));
    }

    @PostMapping("/constituency/hearings/appointments/{id}/convert-case")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Convert attended hearing into a formal grievance case with tracking ID")
    public ApiResponse<Complaint> convertHearingToCase(@PathVariable UUID id) {
        return ApiResponse.ok("Hearing converted to tracked grievance case", civicService.convertHearingToComplaint(id));
    }

    // ==========================================
    // 7. COMPLAINTS LIST & DETAILS BASELINE
    // ==========================================

    @GetMapping("/admin/complaints")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "List paginated complaints for staff dashboard")
    public ApiResponse<PagedResponse<Complaint>> listComplaints(@RequestParam(value = "tenant", required = false) String tenant,
                                                                @RequestParam(value = "tenantId", required = false) UUID tenantId,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PagedResponse.from(civicService.getComplaintsForAdmin(tenant, tenantId, pageable)));
    }

    @GetMapping("/admin/complaints/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Get full case details including updates, field visits, and department referrals")
    public ApiResponse<Map<String, Object>> getComplaintDetails(@PathVariable UUID id) {
        return ApiResponse.ok(civicService.getComplaintDetails(id));
    }

    @PatchMapping("/admin/complaints/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Update complaint case status, assignment, resolution, or public note")
    public ApiResponse<Complaint> updateComplaint(@PathVariable UUID id,
                                                  @RequestBody UpdateComplaintCaseRequest request) {
        return ApiResponse.ok("Complaint updated successfully", civicService.updateComplaintCase(id, request));
    }

    // ==========================================
    // 8. MLA & CONSTITUENCY STAFF PROVISIONING
    // ==========================================

    @GetMapping("/constituency/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMPLAINT_MANAGER', 'MLA')")
    @Operation(summary = "Get list of constituency staff and officers")
    public ApiResponse<List<UserDto>> getStaff() {
        return ApiResponse.ok("Staff list retrieved successfully", civicService.getConstituencyStaff());
    }

    @PostMapping("/constituency/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MLA')")
    @Operation(summary = "Provision a new constituency staff member or grievance officer")
    public ApiResponse<UserDto> provisionStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.ok("Staff member provisioned successfully", civicService.provisionStaffMember(request));
    }

    @PatchMapping("/constituency/staff/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MLA')")
    @Operation(summary = "Activate or deactivate a constituency staff member")
    public ApiResponse<Void> updateStaffStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        civicService.updateStaffStatus(id, active);
        return ApiResponse.ok("Staff status updated successfully", null);
    }
}
