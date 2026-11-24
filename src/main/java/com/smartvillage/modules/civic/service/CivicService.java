package com.smartvillage.modules.civic.service;

import com.smartvillage.common.audit.AuditService;
import com.smartvillage.common.constant.RoleConstants;
import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.util.CodeGenerator;
import com.smartvillage.modules.auth.dto.UserDto;
import com.smartvillage.modules.auth.entity.User;
import com.smartvillage.modules.auth.repository.UserRepository;
import com.smartvillage.modules.civic.dto.*;
import com.smartvillage.modules.civic.entity.*;
import com.smartvillage.modules.civic.repository.*;
import com.smartvillage.modules.tenant.entity.Tenant;
import com.smartvillage.modules.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CivicService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintUpdateRepository complaintUpdateRepository;
    private final ComplaintFieldVisitRepository fieldVisitRepository;
    private final ComplaintFieldVisitItemRepository fieldVisitItemRepository;
    private final DepartmentReferralRepository departmentReferralRepository;
    private final DevelopmentProjectRepository developmentProjectRepository;
    private final PublicHearingRepository publicHearingRepository;
    private final HearingAppointmentRepository hearingAppointmentRepository;
    private final TenantService tenantService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ComplaintPublicTrackingDto submitComplaint(CreateComplaintRequest request) {
        UUID tenantId = request.getTenantId();
        if (tenantId == null) {
            String slug = request.getTenantSlug() != null ? request.getTenantSlug() : TenantContext.getTenantSlug();
            Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
            tenantId = tenant.getId();
        }

        String trackingId = CodeGenerator.generateComplaintTrackingId();
        Instant now = Instant.now();
        Instant dueAt = now.plus(7, ChronoUnit.DAYS); // Standard 7-day initial SLA

        Complaint complaint = Complaint.builder()
                .trackingId(trackingId)
                .citizenName(request.getCitizenName().trim())
                .citizenPhone(request.getCitizenPhone().trim())
                .citizenEmail(request.getCitizenEmail() != null ? request.getCitizenEmail().trim() : null)
                .isAnonymous(request.isAnonymous())
                .category(request.getCategory().trim())
                .wardCode(request.getWardCode())
                .ward(request.getWard())
                .village(request.getVillage())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .priority(request.getPriority() != null ? request.getPriority() : "normal")
                .status("submitted")
                .dueAt(dueAt)
                .isOverdue(false)
                .description(request.getDescription().trim())
                .build();
        complaint.setTenantId(tenantId);

        complaint = complaintRepository.save(complaint);

        // Record initial public update
        ComplaintUpdate initialUpdate = ComplaintUpdate.builder()
                .complaintId(complaint.getId())
                .status("submitted")
                .note("Complaint registered. Assigned tracking ID #" + trackingId + " with 7-day target review SLA.")
                .build();
        initialUpdate.setTenantId(tenantId);
        complaintUpdateRepository.save(initialUpdate);

        return ComplaintPublicTrackingDto.builder()
                .id(complaint.getId())
                .trackingId(trackingId)
                .category(complaint.getCategory())
                .ward(complaint.getWard())
                .village(complaint.getVillage())
                .status(complaint.getStatus())
                .description(complaint.getDescription())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .publicUpdates(List.of(ComplaintUpdateDto.builder()
                        .id(initialUpdate.getId())
                        .status(initialUpdate.getStatus())
                        .note(initialUpdate.getNote())
                        .createdAt(initialUpdate.getCreatedAt())
                        .build()))
                .build();
    }

    @Transactional(readOnly = true)
    public ComplaintPublicTrackingDto trackComplaint(String trackingId, String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        Complaint complaint = complaintRepository.findByTrackingIdAndTenantId(trackingId.trim().toUpperCase(), tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with tracking ID: " + trackingId));

        List<ComplaintUpdateDto> updates = complaintUpdateRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId())
                .stream()
                .map(u -> ComplaintUpdateDto.builder()
                        .id(u.getId())
                        .status(u.getStatus())
                        .note(u.getNote())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();

        return ComplaintPublicTrackingDto.builder()
                .id(complaint.getId())
                .trackingId(complaint.getTrackingId())
                .category(complaint.getCategory())
                .ward(complaint.getWard())
                .village(complaint.getVillage())
                .status(complaint.getStatus())
                .description(complaint.getDescription())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .publicUpdates(updates)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCivicMetrics(String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        long total = complaintRepository.countByTenantId(tenant.getId());
        long resolved = complaintRepository.countByTenantIdAndStatus(tenant.getId(), "resolved");
        long rejected = complaintRepository.countByTenantIdAndStatus(tenant.getId(), "rejected");
        long open = total - (resolved + rejected);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalComplaints", total);
        metrics.put("openComplaints", Math.max(0, open));
        metrics.put("resolvedComplaints", resolved);
        return metrics;
    }

    public List<Tenant> resolveTargetTenants(String tenantSlug) {
        Optional<UserPrincipal> userOpt = SecurityUtils.getCurrentUserOpt();
        if ("all".equalsIgnoreCase(tenantSlug) || "constituency".equalsIgnoreCase(tenantSlug)) {
            if (userOpt.isPresent()) {
                UserPrincipal user = userOpt.get();
                if (!RoleConstants.SUPER_ADMIN.equalsIgnoreCase(user.getRole()) && !RoleConstants.MLA.equalsIgnoreCase(user.getRole())) {
                    throw new ForbiddenException("Only an MLA or Super Administrator has overarching constituency rollup access.");
                }
            }
            List<Tenant> list = tenantService.getTenantEntitiesByConstituency("Garautha");
            if (!list.isEmpty()) {
                return list;
            }
        }
        String slug = (tenantSlug != null && !tenantSlug.isBlank()) ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
        if (userOpt.isPresent()) {
            SecurityUtils.validateTenantAccess(tenant.getId());
        }
        return List.of(tenant);
    }

    // ==========================================
    // 1. CONSTITUENCY COMMAND CENTER (Dashboard)
    // ==========================================
    @Transactional(readOnly = true)
    public Map<String, Object> getConstituencyDashboard(String tenantSlug) {
        List<Tenant> targets = resolveTargetTenants(tenantSlug);
        List<UUID> tenantIds = targets.stream().map(Tenant::getId).toList();
        boolean isAll = targets.size() > 1;

        List<Complaint> allComplaints = isAll
                ? complaintRepository.findByTenantIdInOrderByCreatedAtDesc(tenantIds)
                : complaintRepository.findByTenantIdOrderByCreatedAtDesc(tenantIds.get(0));

        long total = allComplaints.size();
        long submitted = allComplaints.stream().filter(c -> "submitted".equalsIgnoreCase(c.getStatus())).count();
        long underReview = allComplaints.stream().filter(c -> "under_review".equalsIgnoreCase(c.getStatus())).count();
        long inProgress = allComplaints.stream().filter(c -> "in_progress".equalsIgnoreCase(c.getStatus())).count();
        long resolved = allComplaints.stream().filter(c -> "resolved".equalsIgnoreCase(c.getStatus())).count();
        long rejected = allComplaints.stream().filter(c -> "rejected".equalsIgnoreCase(c.getStatus())).count();

        long urgentCount = allComplaints.stream()
                .filter(c -> "urgent".equalsIgnoreCase(c.getPriority()) && !List.of("resolved", "rejected").contains(c.getStatus().toLowerCase()))
                .count();

        long assignedCount = allComplaints.stream()
                .filter(c -> c.getAssignedTo() != null && !List.of("resolved", "rejected").contains(c.getStatus().toLowerCase()))
                .count();

        Instant now = Instant.now();
        long overdueCount = allComplaints.stream()
                .filter(c -> !List.of("resolved", "rejected").contains(c.getStatus().toLowerCase())
                        && (c.isOverdue() || (c.getDueAt() != null && c.getDueAt().isBefore(now)) || (c.getFollowUpAt() != null && c.getFollowUpAt().isBefore(now))))
                .count();

        long totalVisits = isAll ? fieldVisitRepository.countByTenantIdIn(tenantIds) : fieldVisitRepository.countByTenantId(tenantIds.get(0));
        long visitsCompleted = isAll ? fieldVisitRepository.countByTenantIdInAndCompletedAtIsNotNull(tenantIds) : fieldVisitRepository.countByTenantIdAndCompletedAtIsNotNull(tenantIds.get(0));
        long visitsScheduled = Math.max(0, totalVisits - visitsCompleted);

        long departmentReferrals = isAll ? departmentReferralRepository.countByTenantIdIn(tenantIds) : departmentReferralRepository.countByTenantId(tenantIds.get(0));
        long deptWorkInitiated = isAll ? departmentReferralRepository.countByTenantIdInAndStatus(tenantIds, "work_initiated") : departmentReferralRepository.countByTenantIdAndStatus(tenantIds.get(0), "work_initiated");
        long deptVerifiedResolution = isAll ? departmentReferralRepository.countByTenantIdInAndStatus(tenantIds, "verified_resolution") : departmentReferralRepository.countByTenantIdAndStatus(tenantIds.get(0), "verified_resolution");

        // Village pending breakdown
        Map<String, Long> villagePending = new HashMap<>();
        for (Complaint c : allComplaints) {
            if (!List.of("resolved", "rejected").contains(c.getStatus().toLowerCase()) && c.getVillage() != null) {
                villagePending.put(c.getVillage(), villagePending.getOrDefault(c.getVillage(), 0L) + 1);
            }
        }
        List<Map<String, Object>> topVillagesPending = villagePending.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> Map.<String, Object>of("village", e.getKey(), "pendingCount", e.getValue()))
                .toList();

        // Core focus: "Where does the constituency office need to focus its attention today?"
        List<String> focusOfToday = new ArrayList<>();
        if (urgentCount > 0) {
            focusOfToday.add("🚨 " + urgentCount + " urgent citizen grievance(s) require immediate staff review & action.");
        }
        if (overdueCount > 0) {
            focusOfToday.add("⚠️ " + overdueCount + " case(s) have passed target resolution SLA — escalation triggered.");
        }
        if (!topVillagesPending.isEmpty()) {
            Map<String, Object> top = topVillagesPending.get(0);
            focusOfToday.add("📍 Village/Sector " + top.get("village") + " has the highest backlog (" + top.get("pendingCount") + " issues) — prioritize for next field inspection.");
        }
        if (visitsScheduled > 0) {
            focusOfToday.add("🚙 " + visitsScheduled + " field inspection visit(s) currently scheduled on calendar.");
        }
        if (focusOfToday.isEmpty()) {
            focusOfToday.add("✅ All citizen grievances are currently triaged and within target response SLAs.");
        }

        // Available towns in constituency for town switcher
        List<Tenant> allConstituencyTowns = tenantService.getTenantEntitiesByConstituency("Garautha");

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("tenant", isAll ? "all" : targets.get(0).getSlug());
        dashboard.put("tenantName", isAll ? "All Constituency Towns (Garautha Rollup)" : targets.get(0).getName());
        dashboard.put("isConsolidatedRollup", isAll);
        dashboard.put("constituency", "Garautha");
        dashboard.put("availableTowns", allConstituencyTowns.stream().map(t -> Map.of(
                "id", t.getId(),
                "name", t.getName(),
                "slug", t.getSlug(),
                "bodyType", t.getBodyType() != null ? t.getBodyType() : "TOWN"
        )).toList());
        dashboard.put("totalIssues", total);
        dashboard.put("newAwaitingReview", submitted);
        dashboard.put("underReview", underReview);
        dashboard.put("inProgress", inProgress);
        dashboard.put("resolvedIssues", resolved);
        dashboard.put("rejectedIssues", rejected);
        dashboard.put("openIssues", Math.max(0, total - (resolved + rejected)));
        dashboard.put("urgentIssues", urgentCount);
        dashboard.put("assignedToStaff", assignedCount);
        dashboard.put("overdueIssues", overdueCount);
        dashboard.put("fieldVisitsScheduled", visitsScheduled);
        dashboard.put("fieldVisitsCompleted", visitsCompleted);
        dashboard.put("referredToDepartments", departmentReferrals);
        dashboard.put("departmentWorkInitiated", deptWorkInitiated);
        dashboard.put("departmentVerifiedResolution", deptVerifiedResolution);
        dashboard.put("topVillagesPending", topVillagesPending);
        dashboard.put("focusOfTheDay", focusOfToday);

        return dashboard;
    }

    // ==========================================
    // 2. VILLAGE-WISE PROBLEM HEATMAP
    // ==========================================
    @Transactional(readOnly = true)
    public Map<String, Object> getVillageProblemHeatmap(String tenantSlug) {
        List<Tenant> targets = resolveTargetTenants(tenantSlug);
        List<UUID> tenantIds = targets.stream().map(Tenant::getId).toList();
        boolean isAll = targets.size() > 1;

        List<Complaint> complaints = isAll
                ? complaintRepository.findByTenantIdInOrderByCreatedAtDesc(tenantIds)
                : complaintRepository.findByTenantIdOrderByCreatedAtDesc(tenantIds.get(0));

        Map<UUID, String> townNameMap = targets.stream().collect(java.util.stream.Collectors.toMap(Tenant::getId, Tenant::getName));

        Map<String, Map<String, Integer>> matrix = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> villageTotal = new HashMap<>();
        Map<String, Integer> villagePending = new HashMap<>();
        Map<String, Integer> villageResolved = new HashMap<>();
        Set<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Complaint c : complaints) {
            String v = (c.getVillage() != null && !c.getVillage().isBlank()) ? c.getVillage().trim() : "Main Sector";
            String village = isAll ? (townNameMap.getOrDefault(c.getTenantId(), "Town") + " — " + v) : v;
            String category = (c.getCategory() != null && !c.getCategory().isBlank()) ? c.getCategory().trim() : "General";

            categories.add(category);
            matrix.computeIfAbsent(village, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
            matrix.get(village).put(category, matrix.get(village).getOrDefault(category, 0) + 1);

            villageTotal.put(village, villageTotal.getOrDefault(village, 0) + 1);
            if ("resolved".equalsIgnoreCase(c.getStatus())) {
                villageResolved.put(village, villageResolved.getOrDefault(village, 0) + 1);
            } else if (!"rejected".equalsIgnoreCase(c.getStatus())) {
                villagePending.put(village, villagePending.getOrDefault(village, 0) + 1);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> clusters = new ArrayList<>();

        for (Map.Entry<String, Map<String, Integer>> entry : matrix.entrySet()) {
            String vName = entry.getKey();
            Map<String, Integer> catCounts = entry.getValue();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("village", vName);
            row.put("total", villageTotal.getOrDefault(vName, 0));
            row.put("pending", villagePending.getOrDefault(vName, 0));
            row.put("resolved", villageResolved.getOrDefault(vName, 0));
            row.put("categories", catCounts);
            rows.add(row);

            // Problem clusters: >= 3 complaints in the same village and category
            for (Map.Entry<String, Integer> catEntry : catCounts.entrySet()) {
                if (catEntry.getValue() >= 3) {
                    clusters.add(Map.of(
                            "village", vName,
                            "category", catEntry.getKey(),
                            "issueCount", catEntry.getValue(),
                            "recommendation", "Coordinated inspection recommended: " + catEntry.getValue() + " " + catEntry.getKey() + " issues reported in " + vName + "."
                    ));
                }
            }
        }

        rows.sort((a, b) -> Integer.compare((Integer) b.get("pending"), (Integer) a.get("pending")));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenant", isAll ? "all" : targets.get(0).getSlug());
        response.put("categories", categories);
        response.put("heatmap", rows);
        response.put("villageCategoryMatrix", rows);
        response.put("clusters", clusters);
        response.put("problemClusters", clusters);
        return response;
    }

    // ==========================================
    // 3. CONSTITUENCY INTELLIGENCE DASHBOARD
    // ==========================================
    @Transactional(readOnly = true)
    public Map<String, Object> getConstituencyIntelligence(String tenantSlug) {
        List<Tenant> targets = resolveTargetTenants(tenantSlug);
        List<UUID> tenantIds = targets.stream().map(Tenant::getId).toList();
        boolean isAll = targets.size() > 1;

        List<Complaint> complaints = isAll
                ? complaintRepository.findByTenantIdInOrderByCreatedAtDesc(tenantIds)
                : complaintRepository.findByTenantIdOrderByCreatedAtDesc(tenantIds.get(0));
        Instant now = Instant.now();

        // 1. Where are the most reported problems?
        Map<String, Long> villageCounts = new HashMap<>();
        Map<String, Long> categoryCounts = new HashMap<>();
        for (Complaint c : complaints) {
            String v = c.getVillage() != null ? c.getVillage() : "Main Sector";
            String cat = c.getCategory() != null ? c.getCategory() : "General";
            villageCounts.put(v, villageCounts.getOrDefault(v, 0L) + 1);
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0L) + 1);
        }

        // 2. Which issues are remaining unresolved? (Aging analysis)
        long over30Days = 0, between15And30 = 0, between7And14 = 0, under7Days = 0;
        for (Complaint c : complaints) {
            if (!List.of("resolved", "rejected").contains(c.getStatus().toLowerCase())) {
                long days = java.time.Duration.between(c.getCreatedAt(), now).toDays();
                if (days > 30) over30Days++;
                else if (days >= 15) between15And30++;
                else if (days >= 7) between7And14++;
                else under7Days++;
            }
        }

        // 3. Location-based visit recommendations
        Map<String, Object> heatmapData = getVillageProblemHeatmap(tenantSlug);
        Object clusters = heatmapData.get("clusters");

        // 4. Which departments have pending follow-ups?
        List<DepartmentReferral> referrals = isAll
                ? departmentReferralRepository.findByTenantIdInOrderByCreatedAtDesc(tenantIds)
                : departmentReferralRepository.findByTenantIdOrderByCreatedAtDesc(tenantIds.get(0));
        List<DepartmentReferral> pendingDeptFollowups = referrals.stream()
                .filter(r -> List.of("referred", "work_initiated").contains(r.getStatus().toLowerCase()))
                .toList();

        // 5. Measurable progress
        long total = complaints.size();
        long resolved = complaints.stream().filter(c -> "resolved".equalsIgnoreCase(c.getStatus())).count();
        double resolutionRate = total > 0 ? Math.round(((double) resolved / total) * 1000.0) / 10.0 : 0.0;
        long projectsCompleted = isAll
                ? developmentProjectRepository.countByTenantIdInAndStatus(tenantIds, "completed")
                : developmentProjectRepository.countByTenantIdAndStatus(tenantIds.get(0), "completed");

        return Map.of(
                "q1_geographicAnalysis", Map.of("topVillages", villageCounts, "topCategories", categoryCounts),
                "q2_agingAnalysis", Map.of(
                        "over30Days", over30Days,
                        "between15And30Days", between15And30,
                        "between7And14Days", between7And14,
                        "under7Days", under7Days
                ),
                "q3_locationVisitRecommendations", clusters != null ? clusters : List.of(),
                "q4_departmentReferralTracker", Map.of("pendingCount", pendingDeptFollowups.size(), "referrals", pendingDeptFollowups),
                "q5_measurableProgress", Map.of(
                        "totalGrievances", total,
                        "resolvedCount", resolved,
                        "resolutionRatePercent", resolutionRate,
                        "projectsCompleted", projectsCompleted
                )
        );
    }

    // ==========================================
    // 4. FIELD VISITS & BATCH INSPECTIONS
    // ==========================================
    @Transactional
    public ComplaintFieldVisit scheduleFieldVisit(CreateFieldVisitRequest request) {
        Complaint primaryComplaint = complaintRepository.findById(request.getComplaintId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", request.getComplaintId()));
        SecurityUtils.validateTenantAccess(primaryComplaint.getTenantId());

        UUID actorId = SecurityUtils.getCurrentUserId();

        ComplaintFieldVisit visit = ComplaintFieldVisit.builder()
                .complaintId(primaryComplaint.getId())
                .village(request.getVillage() != null ? request.getVillage() : primaryComplaint.getVillage())
                .latitude(request.getLatitude() != null ? request.getLatitude() : primaryComplaint.getLatitude())
                .longitude(request.getLongitude() != null ? request.getLongitude() : primaryComplaint.getLongitude())
                .scheduledAt(request.getScheduledAt())
                .assignedTo(request.getAssignedTo())
                .purpose(request.getPurpose().trim())
                .internalNotes(request.getInternalNotes())
                .actionTaken(request.getActionTaken())
                .pendingAction(request.getPendingAction())
                .beforePhotos(request.getBeforePhotos())
                .afterPhotos(request.getAfterPhotos())
                .nextFollowUpAt(request.getNextFollowUpAt())
                .outcome(request.getOutcome() != null ? request.getOutcome() : "scheduled")
                .createdBy(actorId)
                .build();
        visit.setTenantId(primaryComplaint.getTenantId());
        final ComplaintFieldVisit savedVisit = fieldVisitRepository.save(visit);

        // Batch inspection multi-complaint linkage
        Set<UUID> allComplaintIds = new HashSet<>();
        allComplaintIds.add(primaryComplaint.getId());
        if (request.getRelatedComplaintIds() != null) {
            allComplaintIds.addAll(request.getRelatedComplaintIds());
        }

        for (UUID cId : allComplaintIds) {
            ComplaintFieldVisitItem item = ComplaintFieldVisitItem.builder()
                    .fieldVisitId(savedVisit.getId())
                    .complaintId(cId)
                    .observations("Included in coordinated field inspection #" + savedVisit.getId())
                    .build();
            fieldVisitItemRepository.save(item);

            complaintRepository.findById(cId).ifPresent(c -> {
                if ("submitted".equalsIgnoreCase(c.getStatus()) || "under_review".equalsIgnoreCase(c.getStatus())) {
                    c.setStatus("in_progress");
                    complaintRepository.save(c);
                }
                ComplaintUpdate u = ComplaintUpdate.builder()
                        .complaintId(c.getId())
                        .status("in_progress")
                        .note("Scheduled for field inspection on " + savedVisit.getScheduledAt() + ". Officer assigned.")
                        .createdBy(actorId)
                        .build();
                u.setTenantId(c.getTenantId());
                complaintUpdateRepository.save(u);
            });
        }

        auditService.log(primaryComplaint.getTenantId(), actorId, "SCHEDULE_FIELD_VISIT",
                "complaint_field_visits", savedVisit.getId().toString(),
                "Scheduled inspection visit covering " + allComplaintIds.size() + " complaints");

        return savedVisit;
    }

    @Transactional
    public ComplaintFieldVisit completeFieldVisit(UUID visitId, String outcome, String actionTaken, String pendingAction, String beforePhotos, String afterPhotos, String notes) {
        ComplaintFieldVisit visit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Field visit", "id", visitId));
        SecurityUtils.validateTenantAccess(visit.getTenantId());

        UUID actorId = SecurityUtils.getCurrentUserId();

        visit.setOutcome(outcome != null ? outcome : "completed");
        visit.setCompletedAt(Instant.now());
        if (actionTaken != null) visit.setActionTaken(actionTaken);
        if (pendingAction != null) visit.setPendingAction(pendingAction);
        if (beforePhotos != null) visit.setBeforePhotos(beforePhotos);
        if (afterPhotos != null) visit.setAfterPhotos(afterPhotos);
        if (notes != null) visit.setInternalNotes(notes);

        visit = fieldVisitRepository.save(visit);

        List<ComplaintFieldVisitItem> items = fieldVisitItemRepository.findByFieldVisitId(visitId);
        for (ComplaintFieldVisitItem it : items) {
            ComplaintUpdate u = ComplaintUpdate.builder()
                    .complaintId(it.getComplaintId())
                    .status("in_progress")
                    .note("Field inspection completed: " + (actionTaken != null ? actionTaken : "Inspection observations and evidence recorded."))
                    .createdBy(actorId)
                    .build();
            u.setTenantId(visit.getTenantId());
            complaintUpdateRepository.save(u);
        }

        auditService.log(visit.getTenantId(), actorId, "COMPLETE_FIELD_VISIT",
                "complaint_field_visits", visit.getId().toString(), "Inspection completed with outcome: " + visit.getOutcome());

        return visit;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFieldVisits(String tenantSlug) {
        List<Tenant> targets = resolveTargetTenants(tenantSlug);
        List<UUID> tenantIds = targets.stream().map(Tenant::getId).toList();
        List<ComplaintFieldVisit> visits = targets.size() > 1
                ? fieldVisitRepository.findByTenantIdInOrderByScheduledAtDesc(tenantIds)
                : fieldVisitRepository.findByTenantIdOrderByScheduledAtDesc(tenantIds.get(0));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ComplaintFieldVisit v : visits) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId());
            map.put("complaintId", v.getComplaintId());
            map.put("village", v.getVillage());
            map.put("scheduledAt", v.getScheduledAt());
            map.put("completedAt", v.getCompletedAt());
            map.put("assignedTo", v.getAssignedTo());
            map.put("purpose", v.getPurpose());
            map.put("outcome", v.getOutcome());
            map.put("actionTaken", v.getActionTaken());
            map.put("pendingAction", v.getPendingAction());
            map.put("beforePhotos", v.getBeforePhotos());
            map.put("afterPhotos", v.getAfterPhotos());
            map.put("internalNotes", v.getInternalNotes());

            List<ComplaintFieldVisitItem> items = fieldVisitItemRepository.findByFieldVisitId(v.getId());
            map.put("coveredComplaintCount", items.size());
            map.put("coveredComplaintIds", items.stream().map(ComplaintFieldVisitItem::getComplaintId).toList());
            result.add(map);
        }
        return result;
    }

    // ==========================================
    // 5. DEPARTMENT REFERRAL TRACKER
    // ==========================================
    @Transactional
    public DepartmentReferral createDepartmentReferral(CreateDepartmentReferralRequest request) {
        Complaint complaint = complaintRepository.findById(request.getComplaintId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", request.getComplaintId()));
        SecurityUtils.validateTenantAccess(complaint.getTenantId());
        UUID actorId = SecurityUtils.getCurrentUserId();

        DepartmentReferral referral = DepartmentReferral.builder()
                .complaintId(complaint.getId())
                .departmentName(request.getDepartmentName().trim())
                .officialReferenceNumber(request.getOfficialReferenceNumber())
                .dateOfReferral(request.getDateOfReferral())
                .followUpDate(request.getFollowUpDate())
                .supportingDocuments(request.getSupportingDocuments())
                .responseReceived(request.getResponseReceived())
                .status(request.getStatus() != null ? request.getStatus() : "referred")
                .actionOutcome(request.getActionOutcome())
                .createdBy(actorId)
                .build();
        referral.setTenantId(complaint.getTenantId());
        referral = departmentReferralRepository.save(referral);

        complaint.setDepartment(request.getDepartmentName().trim());
        complaintRepository.save(complaint);

        ComplaintUpdate u = ComplaintUpdate.builder()
                .complaintId(complaint.getId())
                .status("in_progress")
                .note("Matter officially referred to " + request.getDepartmentName() +
                        (request.getOfficialReferenceNumber() != null ? " (Ref #" + request.getOfficialReferenceNumber() + ")" : "") +
                        ". Next follow-up date: " + (request.getFollowUpDate() != null ? request.getFollowUpDate() : "Pending"))
                .createdBy(actorId)
                .build();
        u.setTenantId(complaint.getTenantId());
        complaintUpdateRepository.save(u);

        auditService.log(complaint.getTenantId(), actorId, "CREATE_DEPARTMENT_REFERRAL",
                "constituency_department_referrals", referral.getId().toString(),
                "Referred case #" + complaint.getTrackingId() + " to " + referral.getDepartmentName());

        return referral;
    }

    @Transactional
    public DepartmentReferral updateDepartmentReferral(UUID referralId, UpdateDepartmentReferralRequest request) {
        DepartmentReferral referral = departmentReferralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Department referral", "id", referralId));
        SecurityUtils.validateTenantAccess(referral.getTenantId());
        UUID actorId = SecurityUtils.getCurrentUserId();

        if (request.getOfficialReferenceNumber() != null) referral.setOfficialReferenceNumber(request.getOfficialReferenceNumber());
        if (request.getFollowUpDate() != null) referral.setFollowUpDate(request.getFollowUpDate());
        if (request.getResponseReceived() != null) referral.setResponseReceived(request.getResponseReceived());
        if (request.getStatus() != null) referral.setStatus(request.getStatus());
        if (request.getActionOutcome() != null) referral.setActionOutcome(request.getActionOutcome());

        referral = departmentReferralRepository.save(referral);

        ComplaintUpdate u = ComplaintUpdate.builder()
                .complaintId(referral.getComplaintId())
                .status(referral.getStatus())
                .note("Department update (" + referral.getDepartmentName() + "): Status updated to " + referral.getStatus() +
                        (referral.getResponseReceived() != null ? ". Note: " + referral.getResponseReceived() : ""))
                .createdBy(actorId)
                .build();
        u.setTenantId(referral.getTenantId());
        complaintUpdateRepository.save(u);

        auditService.log(referral.getTenantId(), actorId, "UPDATE_DEPARTMENT_REFERRAL",
                "constituency_department_referrals", referral.getId().toString(),
                "Department referral status updated to " + referral.getStatus());

        return referral;
    }

    @Transactional(readOnly = true)
    public List<DepartmentReferral> getDepartmentReferrals(String tenantSlug) {
        List<Tenant> targets = resolveTargetTenants(tenantSlug);
        List<UUID> tenantIds = targets.stream().map(Tenant::getId).toList();
        if (targets.size() > 1) {
            return departmentReferralRepository.findByTenantIdInOrderByCreatedAtDesc(tenantIds);
        }
        return departmentReferralRepository.findByTenantIdOrderByCreatedAtDesc(tenantIds.get(0));
    }

    // ==========================================
    // 6. PUBLIC DEVELOPMENT PROJECT TRACKER
    // ==========================================
    @Transactional(readOnly = true)
    public List<DevelopmentProject> getDevelopmentProjects(String tenantSlug) {
        List<Tenant> targets = resolveTargetTenants(tenantSlug);
        List<UUID> tenantIds = targets.stream().map(Tenant::getId).toList();
        if (targets.size() > 1) {
            return developmentProjectRepository.findByTenantIdInOrderByCreatedAtDesc(tenantIds);
        }
        return developmentProjectRepository.findByTenantIdOrderByCreatedAtDesc(tenantIds.get(0));
    }

    @Transactional
    public DevelopmentProject createDevelopmentProject(CreateDevelopmentProjectRequest request, String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        UUID actorId = SecurityUtils.getCurrentUserId();

        DevelopmentProject project = DevelopmentProject.builder()
                .projectName(request.getProjectName().trim())
                .village(request.getVillage())
                .ward(request.getWard())
                .category(request.getCategory().trim())
                .responsibleDepartment(request.getResponsibleDepartment())
                .approvedCost(request.getApprovedCost())
                .startDate(request.getStartDate())
                .expectedCompletionDate(request.getExpectedCompletionDate())
                .status(request.getStatus() != null ? request.getStatus() : "planned")
                .milestones(request.getMilestones())
                .photos(request.getPhotos())
                .completionReport(request.getCompletionReport())
                .createdBy(actorId)
                .build();
        project.setTenantId(tenant.getId());

        project = developmentProjectRepository.save(project);
        auditService.log(tenant.getId(), actorId, "CREATE_DEV_PROJECT", "constituency_development_projects", project.getId().toString(), "Created development project: " + project.getProjectName());
        return project;
    }

    @Transactional
    public DevelopmentProject updateDevelopmentProject(UUID id, UpdateDevelopmentProjectRequest request) {
        DevelopmentProject project = developmentProjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Development project", "id", id));
        SecurityUtils.validateTenantAccess(project.getTenantId());
        UUID actorId = SecurityUtils.getCurrentUserId();

        if (request.getProjectName() != null) project.setProjectName(request.getProjectName().trim());
        if (request.getVillage() != null) project.setVillage(request.getVillage().trim());
        if (request.getWard() != null) project.setWard(request.getWard().trim());
        if (request.getCategory() != null) project.setCategory(request.getCategory().trim());
        if (request.getResponsibleDepartment() != null) project.setResponsibleDepartment(request.getResponsibleDepartment());
        if (request.getApprovedCost() != null) project.setApprovedCost(request.getApprovedCost());
        if (request.getStartDate() != null) project.setStartDate(request.getStartDate());
        if (request.getExpectedCompletionDate() != null) project.setExpectedCompletionDate(request.getExpectedCompletionDate());
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        if (request.getMilestones() != null) project.setMilestones(request.getMilestones());
        if (request.getPhotos() != null) project.setPhotos(request.getPhotos());
        if (request.getCompletionReport() != null) project.setCompletionReport(request.getCompletionReport());

        project = developmentProjectRepository.save(project);
        auditService.log(project.getTenantId(), actorId, "UPDATE_DEV_PROJECT", "constituency_development_projects", project.getId().toString(), "Updated project status to: " + project.getStatus());
        return project;
    }

    // ==========================================
    // 7. PUBLIC HEARINGS & APPOINTMENT BOOKING
    // ==========================================
    @Transactional(readOnly = true)
    public List<PublicHearing> getPublicHearings(String tenantSlug) {
        List<Tenant> targets = resolveTargetTenants(tenantSlug);
        List<UUID> tenantIds = targets.stream().map(Tenant::getId).toList();
        if (targets.size() > 1) {
            return publicHearingRepository.findByTenantIdInOrderByHearingDateDesc(tenantIds);
        }
        return publicHearingRepository.findByTenantIdOrderByHearingDateDesc(tenantIds.get(0));
    }

    @Transactional
    public PublicHearing createPublicHearing(CreatePublicHearingRequest request, String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        UUID actorId = SecurityUtils.getCurrentUserId();

        PublicHearing hearing = PublicHearing.builder()
                .hearingDate(request.getHearingDate())
                .hearingTitle(request.getHearingTitle().trim())
                .location(request.getLocation().trim())
                .maxSlots(request.getMaxSlots() > 0 ? request.getMaxSlots() : 50)
                .status("scheduled")
                .notes(request.getNotes())
                .build();
        hearing.setTenantId(tenant.getId());
        return publicHearingRepository.save(hearing);
    }

    @Transactional
    public HearingAppointment requestHearingAppointment(CreateHearingAppointmentRequest request, String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());

        HearingAppointment appt = HearingAppointment.builder()
                .hearingId(request.getHearingId())
                .citizenName(request.getCitizenName().trim())
                .citizenPhone(request.getCitizenPhone().trim())
                .village(request.getVillage())
                .issueCategory(request.getIssueCategory().trim())
                .issueDescription(request.getIssueDescription().trim())
                .preferredDate(request.getPreferredDate())
                .status("requested")
                .build();
        appt.setTenantId(tenant.getId());
        return hearingAppointmentRepository.save(appt);
    }

    @Transactional(readOnly = true)
    public List<HearingAppointment> getHearingAppointments(String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        return hearingAppointmentRepository.findByTenantIdOrderByPreferredDateDesc(tenant.getId());
    }

    @Transactional
    public HearingAppointment updateHearingAppointment(UUID id, String status, Integer slotNumber, String notes) {
        HearingAppointment appt = hearingAppointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hearing appointment", "id", id));
        SecurityUtils.validateTenantAccess(appt.getTenantId());
        if (status != null) appt.setStatus(status);
        if (slotNumber != null) appt.setSlotNumber(slotNumber);
        if (notes != null) appt.setAdminNotes(notes);
        return hearingAppointmentRepository.save(appt);
    }

    @Transactional
    public Complaint convertHearingToComplaint(UUID appointmentId) {
        HearingAppointment appt = hearingAppointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Hearing appointment", "id", appointmentId));
        SecurityUtils.validateTenantAccess(appt.getTenantId());

        CreateComplaintRequest req = new CreateComplaintRequest();
        req.setCitizenName(appt.getCitizenName());
        req.setCitizenPhone(appt.getCitizenPhone());
        req.setCategory(appt.getIssueCategory());
        req.setVillage(appt.getVillage());
        req.setDescription("Originated from Public Hearing on " + appt.getPreferredDate() + ": " + appt.getIssueDescription());
        req.setTenantId(appt.getTenantId());

        ComplaintPublicTrackingDto created = submitComplaint(req);
        Complaint complaint = complaintRepository.findByTrackingIdAndTenantId(created.getTrackingId(), appt.getTenantId()).orElseThrow();

        appt.setFollowUpComplaintId(complaint.getId());
        appt.setStatus("attended");
        hearingAppointmentRepository.save(appt);

        return complaint;
    }

    // ==========================================
    // 8. CITIZEN FEEDBACK & SERVICE REPORTS
    // ==========================================
    @Transactional
    public void submitCitizenFeedback(String trackingId, String tenantSlug, CitizenFeedbackRequest request) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        Complaint complaint = complaintRepository.findByTrackingIdAndTenantId(trackingId.trim().toUpperCase(), tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with tracking ID: " + trackingId));

        complaint.setCitizenFeedbackRating(request.getRating());
        complaint.setCitizenFeedbackNotes(request.getNotes());
        complaintRepository.save(complaint);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConstituencyReport(String tenantSlug, String period) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        UUID tenantId = tenant.getId();
        List<Complaint> complaints = complaintRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);

        long total = complaints.size();
        long resolved = complaints.stream().filter(c -> "resolved".equalsIgnoreCase(c.getStatus())).count();
        long assigned = complaints.stream().filter(c -> c.getAssignedTo() != null).count();
        long overdue = complaints.stream().filter(Complaint::isOverdue).count();

        double avgRating = complaints.stream()
                .filter(c -> c.getCitizenFeedbackRating() != null)
                .mapToInt(Complaint::getCitizenFeedbackRating)
                .average().orElse(0.0);

        return Map.of(
                "tenant", tenant.getSlug(),
                "period", period != null ? period : "quarterly",
                "totalGrievances", total,
                "resolved", resolved,
                "assigned", assigned,
                "overdue", overdue,
                "fieldVisitsCompleted", fieldVisitRepository.countByTenantIdAndCompletedAtIsNotNull(tenantId),
                "departmentReferrals", departmentReferralRepository.countByTenantId(tenantId),
                "averageCitizenSatisfaction", Math.round(avgRating * 10.0) / 10.0,
                "villageDistribution", getVillageProblemHeatmap(tenantSlug).get("heatmap")
        );
    }

    @Transactional(readOnly = true)
    public String exportConstituencyReportCsv(String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        List<Complaint> complaints = complaintRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Tracking ID,Date,Village,Ward,Category,Status,Priority,Department,Resolution Note\n");
        for (Complaint c : complaints) {
            sb.append(escapeCsv(c.getTrackingId())).append(",");
            sb.append(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "").append(",");
            sb.append(escapeCsv(c.getVillage())).append(",");
            sb.append(escapeCsv(c.getWard())).append(",");
            sb.append(escapeCsv(c.getCategory())).append(",");
            sb.append(escapeCsv(c.getStatus())).append(",");
            sb.append(escapeCsv(c.getPriority())).append(",");
            sb.append(escapeCsv(c.getDepartment())).append(",");
            sb.append(escapeCsv(c.getResolutionNote())).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    // ==========================================
    // CASE MANAGEMENT BASELINE
    // ==========================================
    @Transactional(readOnly = true)
    public Page<Complaint> getComplaintsForAdmin(String tenantSlug, UUID tenantId, Pageable pageable) {
        if ("all".equalsIgnoreCase(tenantSlug) || "constituency".equalsIgnoreCase(tenantSlug)) {
            List<Tenant> list = resolveTargetTenants(tenantSlug);
            List<UUID> tenantIds = list.stream().map(Tenant::getId).toList();
            return complaintRepository.findByTenantIdInOrderByCreatedAtDesc(tenantIds, pageable);
        }
        UUID effectiveId = tenantId;
        if (effectiveId == null) {
            effectiveId = resolveTargetTenants(tenantSlug).get(0).getId();
        }
        return complaintRepository.findByTenantIdOrderByCreatedAtDesc(effectiveId, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getComplaintDetails(UUID complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));
        SecurityUtils.validateTenantAccess(complaint.getTenantId());

        List<ComplaintUpdate> updates = complaintUpdateRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
        List<ComplaintFieldVisit> visits = fieldVisitRepository.findByComplaintIdOrderByScheduledAtDesc(complaintId);
        List<DepartmentReferral> referrals = departmentReferralRepository.findByComplaintIdOrderByCreatedAtDesc(complaintId);

        Map<String, Object> result = new HashMap<>();
        result.put("complaint", complaint);
        result.put("updates", updates);
        result.put("fieldVisits", visits);
        result.put("departmentReferrals", referrals);
        return result;
    }

    @Transactional
    public Complaint updateComplaintCase(UUID complaintId, UpdateComplaintCaseRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));
        SecurityUtils.validateTenantAccess(complaint.getTenantId());

        UUID actorId = SecurityUtils.getCurrentUserId();

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            complaint.setStatus(request.getStatus().trim());
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            complaint.setPriority(request.getPriority().trim());
        }
        if (request.getDepartment() != null) {
            complaint.setDepartment(request.getDepartment().trim());
        }
        if (request.getAssignedTo() != null) {
            complaint.setAssignedTo(request.getAssignedTo());
        }
        if (request.getVillage() != null) {
            complaint.setVillage(request.getVillage().trim());
        }
        if (request.getWard() != null) {
            complaint.setWard(request.getWard().trim());
        }
        if (request.getFollowUpAt() != null) {
            complaint.setFollowUpAt(request.getFollowUpAt());
        }
        if (request.getDuplicateOf() != null) {
            complaint.setDuplicateOf(request.getDuplicateOf());
        }
        if (request.getResolutionNote() != null) {
            complaint.setResolutionNote(request.getResolutionNote().trim());
        }

        complaint = complaintRepository.save(complaint);

        if (request.getPublicUpdateNote() != null && !request.getPublicUpdateNote().isBlank()) {
            ComplaintUpdate update = ComplaintUpdate.builder()
                    .complaintId(complaint.getId())
                    .status(complaint.getStatus())
                    .note(request.getPublicUpdateNote().trim())
                    .createdBy(actorId)
                    .build();
            update.setTenantId(complaint.getTenantId());
            complaintUpdateRepository.save(update);
        }

        auditService.log(complaint.getTenantId(), actorId, "UPDATE_CONSTITUENCY_CASE", "complaints", complaintId.toString(), "Case updated with status " + complaint.getStatus());

        return complaint;
    }

    // ==========================================
    // 8. MLA & CONSTITUENCY STAFF PROVISIONING
    // ==========================================

    @Transactional
    public UserDto provisionStaffMember(CreateStaffRequest request) {
        UserPrincipal caller = SecurityUtils.getCurrentUser();
        UUID tenantId = caller.getTenantId();
        if (tenantId == null) {
            tenantId = tenantService.getTenantEntityBySlug("gursarai").getId();
        }

        String email = request.getEmail().toLowerCase().trim();
        String phone = request.getPhone().replaceAll("\\D", "").trim();
        String role = request.getRole().toUpperCase().trim();

        List<String> allowedRoles;
        if (RoleConstants.SUPER_ADMIN.equalsIgnoreCase(caller.getRole())) {
            allowedRoles = List.of(
                    RoleConstants.SUPER_ADMIN,
                    RoleConstants.ADMIN,
                    RoleConstants.MLA,
                    RoleConstants.COMPLAINT_MANAGER,
                    RoleConstants.COMMUNICATIONS,
                    RoleConstants.OPERATIONS_MANAGER,
                    RoleConstants.VOLUNTEER_MANAGER,
                    RoleConstants.CONTENT_EDITOR
            );
        } else if (RoleConstants.ADMIN.equalsIgnoreCase(caller.getRole()) || RoleConstants.MLA.equalsIgnoreCase(caller.getRole())) {
            allowedRoles = List.of(
                    RoleConstants.COMPLAINT_MANAGER,
                    RoleConstants.COMMUNICATIONS,
                    RoleConstants.OPERATIONS_MANAGER,
                    RoleConstants.VOLUNTEER_MANAGER,
                    RoleConstants.CONTENT_EDITOR,
                    RoleConstants.ADMIN
            );
        } else {
            throw new ForbiddenException("Only administrators or MLAs can provision constituency staff.");
        }

        if (!allowedRoles.contains(role)) {
            throw new BadRequestException("Role '" + role + "' cannot be assigned by your current administrative privilege.");
        }

        Optional<User> existingByEmail = userRepository.findByEmail(email);
        Optional<User> existingByPhone = userRepository.findByPhone(phone);

        if (existingByEmail.isPresent() && existingByPhone.isPresent() && !existingByEmail.get().getId().equals(existingByPhone.get().getId())) {
            throw new ConflictException("Email and phone number belong to two different registered accounts.");
        }

        User user;
        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword().trim()
                : "Staff@" + phone.substring(Math.max(0, phone.length() - 4)) + "!";

        if (existingByPhone.isPresent()) {
            user = existingByPhone.get();
            user.setRole(role);
            user.setFullName(request.getFullName().trim());
            user.setEmail(email);
            user.setTenantId(tenantId);
            user.setActive(true);
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
            }
            log.info("Promoted existing user {} to staff role {}", phone, role);
        } else if (existingByEmail.isPresent()) {
            user = existingByEmail.get();
            user.setRole(role);
            user.setFullName(request.getFullName().trim());
            user.setPhone(phone);
            user.setTenantId(tenantId);
            user.setActive(true);
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
            }
            log.info("Promoted existing user with email {} to staff role {}", email, role);
        } else {
            user = User.builder()
                    .email(email)
                    .phone(phone)
                    .fullName(request.getFullName().trim())
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .tenantId(tenantId)
                    .active(true)
                    .build();
            log.info("Provisioned new staff user {} with role {}", phone, role);
        }

        user = userRepository.save(user);

        auditService.log(tenantId, caller.getId(), "PROVISION_STAFF", "users", user.getId().toString(),
                "Provisioned/Updated staff member " + user.getFullName() + " with role " + role + " and mobile " + phone);

        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getConstituencyStaff() {
        UserPrincipal caller = SecurityUtils.getCurrentUser();
        UUID tenantId = caller.getTenantId();
        if (tenantId == null) {
            tenantId = tenantService.getTenantEntityBySlug("gursarai").getId();
        }

        List<User> users = userRepository.findByTenantId(tenantId);
        List<String> staffRoles = List.of(
                RoleConstants.SUPER_ADMIN,
                RoleConstants.ADMIN,
                RoleConstants.MLA,
                RoleConstants.COMPLAINT_MANAGER,
                RoleConstants.COMMUNICATIONS,
                RoleConstants.OPERATIONS_MANAGER,
                RoleConstants.VOLUNTEER_MANAGER,
                RoleConstants.CONTENT_EDITOR
        );

        return users.stream()
                .filter(u -> staffRoles.contains(u.getRole().toUpperCase()))
                .map(u -> UserDto.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .role(u.getRole())
                        .tenantId(u.getTenantId())
                        .active(u.isActive())
                        .createdAt(u.getCreatedAt())
                        .build())
                .sorted(Comparator.comparing(UserDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public void updateStaffStatus(UUID userId, boolean active) {
        UserPrincipal caller = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff User", "id", userId));

        if (!RoleConstants.SUPER_ADMIN.equalsIgnoreCase(caller.getRole())) {
            if (user.getTenantId() != null && !user.getTenantId().equals(caller.getTenantId())) {
                throw new ForbiddenException("Cannot modify staff belonging to another constituency.");
            }
            if (RoleConstants.SUPER_ADMIN.equalsIgnoreCase(user.getRole()) ||
                RoleConstants.ADMIN.equalsIgnoreCase(user.getRole()) ||
                RoleConstants.MLA.equalsIgnoreCase(user.getRole())) {
                throw new ForbiddenException("Only SUPER_ADMIN can modify status of an MLA or Admin account.");
            }
        }

        user.setActive(active);
        userRepository.save(user);

        auditService.log(user.getTenantId(), caller.getId(), "UPDATE_STAFF_STATUS", "users", user.getId().toString(),
                "Updated staff member status for " + user.getFullName() + " to active=" + active);
    }
}
