package com.smartvillage.modules.marketplace.service;

import com.smartvillage.common.audit.AuditService;
import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.storage.StorageService;
import com.smartvillage.common.util.CodeGenerator;
import com.smartvillage.common.util.SlugUtils;
import com.smartvillage.modules.auth.entity.User;
import com.smartvillage.modules.auth.repository.UserRepository;
import com.smartvillage.modules.delivery.entity.DeliveryAgent;
import com.smartvillage.modules.delivery.repository.DeliveryAgentRepository;
import com.smartvillage.modules.delivery.service.DeliveryService;
import com.smartvillage.modules.marketplace.entity.*;
import com.smartvillage.modules.marketplace.repository.*;
import com.smartvillage.modules.payment.entity.PaymentSubmission;
import com.smartvillage.modules.payment.repository.PaymentSubmissionRepository;
import com.smartvillage.modules.tenant.dto.TenantDto;
import com.smartvillage.modules.tenant.entity.Tenant;
import com.smartvillage.modules.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final TenantService tenantService;
    private final CategoryRepository categoryRepository;
    private final CategoryRequestRepository categoryRequestRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final AuditService auditService;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final PaymentSubmissionRepository paymentSubmissionRepository;
    private final DeliveryService deliveryService;

    @Transactional(readOnly = true)
    public Map<String, Object> getPublicVendorRegistrationData(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug.trim().toLowerCase() : "gursarai";
        List<TenantDto> tenants = tenantService.getActiveTenants();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug);

        List<Category> categories = categoryRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(tenant.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("tenants", tenants);
        result.put("chosenTenant", tenant.getSlug());
        result.put("categories", categories);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Category> getActiveCategories(String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        return categoryRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(tenant.getId());
    }

    @Transactional
    public Category createCategory(String name, String nameHi, String icon, String description, Integer sortOrder, String tenantSlug) {
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug());
        SecurityUtils.validateTenantAccess(tenant.getId());

        String slug = SlugUtils.toSlug(name);
        if (categoryRepository.existsByTenantIdAndSlug(tenant.getId(), slug)) {
            throw new ConflictException("A category with this name already exists in this marketplace.");
        }

        Category category = Category.builder()
                .name(name.trim())
                .nameHi(nameHi != null ? nameHi.trim() : null)
                .slug(slug)
                .icon(icon != null ? icon.trim() : "🛍️")
                .description(description != null ? description.trim() : null)
                .sortOrder(sortOrder != null ? sortOrder : 100)
                .active(true)
                .build();
        category.setTenantId(tenant.getId());

        return categoryRepository.save(category);
    }

    @Transactional
    public CategoryRequest requestCategory(String requestedName, String reason) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getVendorId() == null) {
            throw new ForbiddenException("Active vendor account is required.");
        }

        Vendor vendor = vendorRepository.findById(user.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", user.getVendorId()));

        CategoryRequest request = CategoryRequest.builder()
                .vendorId(vendor.getId())
                .requestedName(requestedName.trim())
                .reason(reason != null ? reason.trim() : null)
                .status("pending")
                .build();
        request.setTenantId(vendor.getTenantId());

        return categoryRequestRepository.save(request);
    }

    @Transactional
    public Map<String, Object> reviewCategoryRequest(UUID requestId, String status, String reviewNote) {
        CategoryRequest request = categoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryRequest", "id", requestId));
        SecurityUtils.validateTenantAccess(request.getTenantId());

        if (!"pending".equalsIgnoreCase(request.getStatus())) {
            throw new ConflictException("This request has already been reviewed.");
        }

        UUID reviewerId = SecurityUtils.getCurrentUserId();
        request.setStatus(status.toLowerCase());
        request.setReviewedBy(reviewerId);
        request.setReviewedAt(Instant.now());
        request.setReviewNote(reviewNote);

        Category createdCategory = null;
        if ("approved".equalsIgnoreCase(status)) {
            String slug = SlugUtils.toSlug(request.getRequestedName());
            Optional<Category> existing = categoryRepository.findByTenantIdAndSlug(request.getTenantId(), slug);
            if (existing.isPresent()) {
                createdCategory = existing.get();
            } else {
                createdCategory = Category.builder()
                        .name(request.getRequestedName())
                        .slug(slug)
                        .icon("🛍️")
                        .description("Category approved from vendor request: " + request.getRequestedName())
                        .active(true)
                        .build();
                createdCategory.setTenantId(request.getTenantId());
                createdCategory = categoryRepository.save(createdCategory);
            }
            request.setCategoryId(createdCategory.getId());
        }

        categoryRequestRepository.save(request);

        Map<String, Object> response = new HashMap<>();
        response.put("request", request);
        response.put("category", createdCategory);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVendorDashboard() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getVendorId() == null) {
            throw new ForbiddenException("Active vendor account is required.");
        }

        Vendor vendor = vendorRepository.findById(user.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", user.getVendorId()));

        List<Order> orders = orderRepository.findByVendorIdOrderByCreatedAtDesc(vendor.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("vendor", vendor);
        result.put("orders", orders);
        return result;
    }

    @Transactional
    public void updateVendorOrderStatus(UUID orderId, String newStatus) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getVendorId() == null) {
            throw new ForbiddenException("Active vendor account is required.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getVendorId().equals(user.getVendorId())) {
            throw new ForbiddenException("This order does not belong to your store.");
        }

        List<String> allowedTransitions = List.of("placed", "accepted", "processing", "ready", "rejected", "cancelled");
        if (!allowedTransitions.contains(newStatus.toLowerCase())) {
            throw new BadRequestException("Invalid status transition. Delivery status is controlled by assigned delivery partners.");
        }

        if (List.of("out_for_delivery", "delivered", "rejected", "cancelled").contains(order.getOrderStatus().toLowerCase())) {
            throw new ConflictException("This order is in a terminal or delivery-managed state and cannot be changed by vendor.");
        }

        if ("upi".equalsIgnoreCase(order.getPaymentMethod()) && !"paid".equalsIgnoreCase(order.getPaymentStatus()) &&
                List.of("accepted", "processing", "ready").contains(newStatus.toLowerCase())) {
            throw new ConflictException("Verify customer's upfront UPI payment before accepting or preparing this order.");
        }

        order.setOrderStatus(newStatus.toLowerCase());
        order = orderRepository.save(order);

        if ("ready".equalsIgnoreCase(newStatus)) {
            try {
                deliveryService.broadcastOrderReady(order);
            } catch (Exception e) {
                log.warn("Failed to broadcast ready order {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }
    }

    @Transactional
    public Map<String, String> updateVendorQr(MultipartFile file, String upiId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getVendorId() == null) {
            throw new ForbiddenException("Active vendor account is required.");
        }

        Vendor vendor = vendorRepository.findById(user.getVendorId()).orElseThrow();

        if (upiId != null && !upiId.isBlank()) {
            if (!upiId.matches("^[\\w.+-]+@[\\w.-]+$")) {
                throw new BadRequestException("Enter a valid UPI ID, for example shop@upi.");
            }
            vendor.setUpiId(upiId.trim());
        }

        if (file != null && !file.isEmpty()) {
            String path = storageService.store(file, "vendors/" + vendor.getId() + "/upi-qr");
            String url = storageService.resolveUrl(path);
            vendor.setUpiQrUrl(url);
        }

        vendorRepository.save(vendor);

        Map<String, String> result = new HashMap<>();
        result.put("upiId", vendor.getUpiId());
        result.put("upiQrUrl", vendor.getUpiQrUrl());
        return result;
    }

    @Transactional
    public Map<String, Object> reviewCustomerRequest(UUID customerAuthId, String status) {
        CustomerProfile profile = customerProfileRepository.findByAuthUserId(customerAuthId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer request not found for auth ID: " + customerAuthId));

        SecurityUtils.validateTenantAccess(profile.getTenantId());

        if (!"pending".equalsIgnoreCase(profile.getApprovalStatus())) {
            throw new ConflictException("This customer request has already been reviewed.");
        }

        UUID reviewerId = SecurityUtils.getCurrentUserId();
        profile.setReviewedBy(reviewerId);
        profile.setReviewedAt(Instant.now());

        if ("rejected".equalsIgnoreCase(status)) {
            profile.setApprovalStatus("rejected");
            customerProfileRepository.save(profile);
            return Map.of("status", "rejected");
        }

        // Approved
        String pin = CodeGenerator.generateCustomerPin();
        User user = userRepository.findById(customerAuthId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerAuthId));

        user.setPassword(passwordEncoder.encode(pin));
        user.setActive(true);
        userRepository.save(user);

        profile.setApprovalStatus("approved");
        profile.setApprovedAt(Instant.now());
        customerProfileRepository.save(profile);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "approved");
        response.put("customer", Map.of("full_name", profile.getFullName(), "phone", profile.getPhone()));
        response.put("customerName", profile.getFullName());
        response.put("customerPhone", profile.getPhone());
        response.put("pin", pin);
        response.put("warning", "PIN is shown only once. Share it with the customer after confirming their identity.");
        return response;
    }

    @Transactional
    public void removeVendorQr() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getVendorId() == null) {
            throw new ForbiddenException("Active vendor account is required.");
        }
        Vendor vendor = vendorRepository.findById(user.getVendorId()).orElseThrow();
        vendor.setUpiQrUrl(null);
        vendorRepository.save(vendor);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminCategories(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
        List<Category> categories = categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenant.getId());
        List<CategoryRequest> requests = categoryRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());

        List<Map<String, Object>> requestDtos = new ArrayList<>();
        for (CategoryRequest r : requests) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("vendor_id", r.getVendorId());
            m.put("requested_name", r.getRequestedName());
            m.put("reason", r.getReason());
            m.put("status", r.getStatus());
            m.put("created_at", r.getCreatedAt());
            m.put("reviewed_at", r.getReviewedAt());
            m.put("review_note", r.getReviewNote());
            vendorRepository.findById(r.getVendorId()).ifPresent(v -> m.put("vendor_name", v.getBusinessName()));
            requestDtos.add(m);
        }
        return Map.of(
                "categories", categories,
                "requests", requestDtos,
                "tenant", Map.of("id", tenant.getId(), "name", tenant.getName(), "slug", tenant.getSlug())
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminOrders(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
        List<Order> orders = orderRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());
        List<DeliveryAgent> agents = deliveryAgentRepository.findByTenantIdAndIsActiveTrue(tenant.getId());
        return Map.of("orders", orders, "agents", agents);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminPayments(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
        List<PaymentSubmission> payments = paymentSubmissionRepository.findByTenantIdOrderBySubmittedAtDesc(tenant.getId());
        List<Vendor> vendors = vendorRepository.findByTenantIdOrderByBusinessNameAsc(tenant.getId());

        Map<UUID, Vendor> vendorMap = vendors.stream().collect(Collectors.toMap(Vendor::getId, v -> v, (a, b) -> a));
        List<Map<String, Object>> paymentDtos = new ArrayList<>();
        for (PaymentSubmission p : payments) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("order_id", p.getOrderId());
            m.put("vendor_id", p.getVendorId());
            m.put("amount", p.getAmount());
            m.put("utr_number", p.getUtrNumber());
            m.put("proof_path", p.getProofPath());
            m.put("proof_url", p.getProofPath() != null ? storageService.resolveUrl(p.getProofPath()) : null);
            m.put("status", p.getStatus());
            m.put("submitted_at", p.getSubmittedAt());
            m.put("reviewed_at", p.getReviewedAt());
            m.put("review_note", p.getReviewNote());
            Vendor v = vendorMap.get(p.getVendorId());
            if (v != null) {
                m.put("vendor", Map.of("id", v.getId(), "business_name", v.getBusinessName()));
            }
            paymentDtos.add(m);
        }
        return Map.of("payments", paymentDtos, "vendors", vendors);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminCustomerRequests(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
        List<CustomerProfile> pendingCustomers = customerProfileRepository
                .findByTenantIdAndApprovalStatusOrderByCreatedAtAsc(tenant.getId(), "pending");

        List<Map<String, Object>> customerDtos = new ArrayList<>();
        for (CustomerProfile c : pendingCustomers) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("auth_user_id", c.getAuthUserId());
            m.put("full_name", c.getFullName());
            m.put("phone", c.getPhone());
            m.put("created_at", c.getCreatedAt());
            List<Order> orders = orderRepository.findByCustomerAuthIdOrderByCreatedAtDesc(c.getAuthUserId());
            m.put("orders", orders);
            customerDtos.add(m);
        }
        return Map.of("customers", customerDtos);
    }
}
