package com.smartvillage.modules.delivery.service;

import com.smartvillage.common.audit.AuditService;
import com.smartvillage.common.constant.RoleConstants;
import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.storage.StorageService;
import com.smartvillage.common.util.CodeGenerator;
import com.smartvillage.modules.auth.entity.User;
import com.smartvillage.modules.auth.repository.UserRepository;
import com.smartvillage.modules.delivery.entity.*;
import com.smartvillage.modules.delivery.repository.*;
import com.smartvillage.modules.marketplace.entity.Order;
import com.smartvillage.modules.marketplace.entity.Vendor;
import com.smartvillage.modules.marketplace.repository.OrderRepository;
import com.smartvillage.modules.marketplace.repository.VendorRepository;
import com.smartvillage.modules.notification.entity.Notification;
import com.smartvillage.modules.notification.repository.NotificationRepository;
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
public class DeliveryService {

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryCodeRepository deliveryCodeRepository;
    private final DeliveryReviewRepository deliveryReviewRepository;
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final AuditService auditService;

    @Transactional
    public DeliveryAssignment assignDelivery(UUID orderId, UUID agentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!"ready".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Assign the delivery agent after the vendor marks the order Ready.");
        }

        if (order.getDeliveryAgentId() != null) {
            throw new ConflictException("A delivery agent is already assigned to this order.");
        }

        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAgent", "id", agentId));

        if (!agent.isActive()) {
            throw new BadRequestException("Selected delivery agent is not active.");
        }

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(order.getId())
                .deliveryAgentId(agent.getId())
                .status("assigned")
                .assignedAt(Instant.now())
                .build();

        order.setDeliveryAgentId(agent.getId());
        orderRepository.save(order);

        auditService.log(order.getTenantId(), SecurityUtils.getCurrentUserId(), "ASSIGN_DELIVERY",
                "marketplace_orders", order.getId().toString(), "Assigned to driver: " + agent.getFullName());

        return deliveryAssignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAgentMe() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery agent account not found."));

        List<DeliveryAssignment> assignments = deliveryAssignmentRepository.findByDeliveryAgentIdOrderByAssignedAtDesc(agent.getId());
        List<DeliveryReview> reviews = deliveryReviewRepository.findByDeliveryAgentIdOrderByCreatedAtDesc(agent.getId());

        double avgRating = reviews.stream().mapToInt(DeliveryReview::getRating).average().orElse(0.0);

        // Enrich assignments with order and vendor details for seamless UI display
        Set<UUID> orderIds = assignments.stream().map(DeliveryAssignment::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, Order> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderRepository.findAllById(orderIds).forEach(o -> orderMap.put(o.getId(), o));
        }
        Set<UUID> vendorIds = orderMap.values().stream().map(Order::getVendorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, Vendor> vendorMap = new HashMap<>();
        if (!vendorIds.isEmpty()) {
            vendorRepository.findAllById(vendorIds).forEach(v -> vendorMap.put(v.getId(), v));
        }

        List<Map<String, Object>> enrichedAssignments = new ArrayList<>();
        for (DeliveryAssignment a : assignments) {
            Map<String, Object> aMap = new LinkedHashMap<>();
            aMap.put("id", a.getId());
            aMap.put("order_id", a.getOrderId());
            aMap.put("delivery_agent_id", a.getDeliveryAgentId());
            aMap.put("status", a.getStatus());
            aMap.put("assigned_at", a.getAssignedAt());
            aMap.put("accepted_at", a.getAcceptedAt());
            aMap.put("picked_up_at", a.getPickedUpAt());
            aMap.put("delivered_at", a.getDeliveredAt());
            aMap.put("delivery_note", a.getDeliveryNote());

            Order ord = orderMap.get(a.getOrderId());
            if (ord != null) {
                Map<String, Object> oMap = new LinkedHashMap<>();
                oMap.put("id", ord.getId());
                oMap.put("order_number", ord.getOrderNumber());
                oMap.put("order_status", ord.getOrderStatus());
                oMap.put("payment_status", ord.getPaymentStatus());
                oMap.put("payment_method", ord.getPaymentMethod());
                oMap.put("total", ord.getTotal());
                oMap.put("customer_name", ord.getCustomerName());
                oMap.put("customer_phone", ord.getCustomerPhone());
                oMap.put("delivery_address", ord.getDeliveryAddress());

                Vendor vend = vendorMap.get(ord.getVendorId());
                if (vend != null) {
                    Map<String, Object> vObj = new LinkedHashMap<>();
                    vObj.put("id", vend.getId());
                    vObj.put("business_name", vend.getBusinessName() != null ? vend.getBusinessName() : "Vendor");
                    vObj.put("address", vend.getAddress() != null ? vend.getAddress() : "");
                    vObj.put("phone", vend.getPhone() != null ? vend.getPhone() : "");
                    vObj.put("location_label", vend.getLocationLabel());
                    vObj.put("location_latitude", vend.getLocationLatitude());
                    vObj.put("location_longitude", vend.getLocationLongitude());
                    oMap.put("vendor", vObj);
                }
                aMap.put("order", oMap);
            }
            enrichedAssignments.add(aMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("agent", agent);
        result.put("assignments", enrichedAssignments);
        result.put("available_orders", getAvailableOrders());
        result.put("reviews", reviews);
        result.put("ratingAverage", Math.round(avgRating * 10.0) / 10.0);
        result.put("ratingCount", reviews.size());
        return result;
    }

    @Transactional
    public void broadcastOrderReady(Order order) {
        if (order == null || order.getTenantId() == null) {
            return;
        }
        if (order.getDeliveryMethod() != null && !"delivery".equalsIgnoreCase(order.getDeliveryMethod())) {
            return;
        }

        List<DeliveryAgent> agents = deliveryAgentRepository.findByTenantIdAndIsActiveTrue(order.getTenantId());
        if (agents.isEmpty()) {
            log.info("No active delivery agents found in tenant {} for order {}", order.getTenantId(), order.getOrderNumber());
            return;
        }

        List<Notification> notifications = new ArrayList<>();
        for (DeliveryAgent ag : agents) {
            if (ag.getAuthUserId() != null) {
                Notification n = Notification.builder()
                        .recipientAuthId(ag.getAuthUserId())
                        .recipientType("delivery")
                        .title("🔔 New Delivery Job Available!")
                        .message("Order #" + order.getOrderNumber() + " is ready for pickup! Tap to claim this delivery.")
                        .build();
                n.setTenantId(order.getTenantId());
                notifications.add(n);
            }
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("Broadcasted ready order {} to {} delivery agents in tenant {}",
                    order.getOrderNumber(), notifications.size(), order.getTenantId());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableOrders() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElse(null);

        UUID tenantId = (agent != null && agent.getTenantId() != null) ? agent.getTenantId() : user.getTenantId();
        if (tenantId == null) {
            return Collections.emptyList();
        }

        List<Order> orders = orderRepository.findAvailableOrdersForDelivery(tenantId);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> vendorIds = orders.stream().map(Order::getVendorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, Vendor> vendorMap = new HashMap<>();
        if (!vendorIds.isEmpty()) {
            vendorRepository.findAllById(vendorIds).forEach(v -> vendorMap.put(v.getId(), v));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("order_id", o.getId());
            map.put("order_number", o.getOrderNumber());
            map.put("created_at", o.getCreatedAt());
            map.put("total", o.getTotal());
            map.put("subtotal", o.getSubtotal());
            map.put("delivery_charge", o.getDeliveryCharge());
            map.put("payment_method", o.getPaymentMethod());
            map.put("payment_status", o.getPaymentStatus());
            map.put("order_status", o.getOrderStatus());
            map.put("delivery_address", o.getDeliveryAddress());
            map.put("customer_name", o.getCustomerName());
            map.put("customer_phone", o.getCustomerPhone());

            Vendor v = vendorMap.get(o.getVendorId());
            if (v != null) {
                map.put("vendor_id", v.getId());
                map.put("vendor_name", v.getBusinessName());
                map.put("vendor_address", v.getAddress());
                map.put("vendor_phone", v.getPhone());
                map.put("vendor_location_label", v.getLocationLabel());
                map.put("vendor_latitude", v.getLocationLatitude());
                map.put("vendor_longitude", v.getLocationLongitude());
            } else {
                map.put("vendor_id", o.getVendorId());
                map.put("vendor_name", "Vendor");
                map.put("vendor_address", null);
                map.put("vendor_phone", null);
            }
            map.put("items_count", o.getItems() != null ? o.getItems().size() : 0);
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> claimOrder(UUID orderId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery agent profile not found."));

        if (!agent.isActive()) {
            throw new ForbiddenException("Your delivery agent account is currently inactive.");
        }

        // Row-level lock via SELECT FOR UPDATE to prevent race conditions
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getTenantId() != null && agent.getTenantId() != null && !order.getTenantId().equals(agent.getTenantId())) {
            throw new ForbiddenException("You cannot claim an order from another village or constituency.");
        }

        if (!"ready".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BadRequestException("Order is not ready for delivery pickup (Current status: " + order.getOrderStatus() + ").");
        }

        if (order.getDeliveryAgentId() != null) {
            if (order.getDeliveryAgentId().equals(agent.getId())) {
                throw new BadRequestException("You have already claimed this order.");
            }
            throw new ConflictException("Too late! This order has already been claimed by another delivery partner.");
        }

        Optional<DeliveryAssignment> existing = deliveryAssignmentRepository.findByOrderId(order.getId());
        if (existing.isPresent() && !List.of("delivered", "cancelled").contains(existing.get().getStatus().toLowerCase())) {
            throw new ConflictException("Too late! This order has already been claimed by another delivery partner.");
        }

        order.setDeliveryAgentId(agent.getId());
        order = orderRepository.save(order);

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(order.getId())
                .deliveryAgentId(agent.getId())
                .status("accepted")
                .assignedAt(Instant.now())
                .acceptedAt(Instant.now())
                .build();
        assignment = deliveryAssignmentRepository.save(assignment);

        auditService.log(order.getTenantId(), user.getId(), "CLAIM_DELIVERY_ORDER",
                "marketplace_orders", order.getId().toString(),
                "Claimed by driver: " + agent.getFullName());

        log.info("Order {} claimed by driver {} ({})", order.getOrderNumber(), agent.getFullName(), agent.getId());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Order claimed successfully! Head to vendor for pickup.");
        resp.put("assignment", assignment);
        resp.put("order", Map.of(
                "id", order.getId(),
                "order_number", order.getOrderNumber(),
                "order_status", order.getOrderStatus(),
                "delivery_address", order.getDeliveryAddress()
        ));
        return resp;
    }

    @Transactional
    public void updateAgentAvailability(String status) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery agent account not found."));

        List<String> allowed = List.of("offline", "available", "busy");
        if (!allowed.contains(status.toLowerCase())) {
            throw new BadRequestException("Invalid availability status: " + status);
        }

        agent.setAvailabilityStatus(status.toLowerCase());
        deliveryAgentRepository.save(agent);
    }

    @Transactional
    public Map<String, Object> updateAssignmentStatus(UUID assignmentId, String newStatus) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery agent account not found."));

        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAssignment", "id", assignmentId));

        if (!assignment.getDeliveryAgentId().equals(agent.getId())) {
            throw new ForbiddenException("You are not assigned to this delivery.");
        }

        final UUID targetOrderId = assignment.getOrderId();
        Order order = orderRepository.findById(targetOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", targetOrderId));

        String targetStatus = newStatus.toLowerCase();
        boolean codeGenerated = false;

        if ("out_for_delivery".equals(targetStatus)) {
            if (!"paid".equalsIgnoreCase(order.getPaymentStatus())) {
                String msg = "upi".equalsIgnoreCase(order.getPaymentMethod())
                        ? "Payment is still pending. The customer must pay the vendor by UPI and payment must be verified before moving to Out for delivery."
                        : "Payment is not confirmed. This order cannot be marked Out for delivery until payment is confirmed.";
                throw new ConflictException(msg);
            }

            // Generate delivery OTP code
            String otp = CodeGenerator.generateDeliveryOtp();
            String hash = CodeGenerator.hashOtp(otp);

            order.setDeliveryOtpHash(hash);
            order.setDeliveryOtpSentAt(Instant.now());
            order.setOrderStatus("out_for_delivery");
            orderRepository.save(order);

            DeliveryCode deliveryCode = DeliveryCode.builder()
                    .orderId(order.getId())
                    .code(otp)
                    .updatedAt(Instant.now())
                    .build();
            deliveryCodeRepository.save(deliveryCode);

            codeGenerated = true;
        } else if ("picked_up".equals(targetStatus)) {
            assignment.setPickedUpAt(Instant.now());
        } else if ("accepted".equals(targetStatus)) {
            assignment.setAcceptedAt(Instant.now());
        }

        assignment.setStatus(targetStatus);
        assignment = deliveryAssignmentRepository.save(assignment);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("status", targetStatus);
        res.put("deliveryCodeGenerated", codeGenerated);
        return res;
    }

    @Transactional
    public Order recordCashPayment(UUID orderId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery agent account not found."));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!agent.getId().equals(order.getDeliveryAgentId())) {
            throw new ForbiddenException("Not authorized for this delivery payment.");
        }

        if (!List.of("cod", "pay_at_pickup").contains(order.getPaymentMethod().toLowerCase())) {
            throw new BadRequestException("This order is not a cash on delivery / pay at pickup order.");
        }

        order.setPaymentStatus("paid");
        order.setPaymentCollectedAt(Instant.now());
        order.setPaymentCollectedBy(user.getId());
        return orderRepository.save(order);
    }

    @Transactional
    public Map<String, Object> completeDelivery(UUID assignmentId, String otp, String note) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery agent account not found."));

        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAssignment", "id", assignmentId));

        if (!assignment.getDeliveryAgentId().equals(agent.getId())) {
            throw new ForbiddenException("You are not assigned to this delivery.");
        }

        final UUID targetOrderId = assignment.getOrderId();
        Order order = orderRepository.findById(targetOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", targetOrderId));

        if (!"paid".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new ConflictException("Payment is not confirmed. This order cannot be delivered until payment is Paid.");
        }

        // Verify OTP
        String cleanedOtp = otp != null ? otp.trim() : "";
        DeliveryCode storedCode = deliveryCodeRepository.findById(order.getId()).orElse(null);
        String calculatedHash = CodeGenerator.hashOtp(cleanedOtp);

        boolean match = (storedCode != null && storedCode.getCode().equals(cleanedOtp)) ||
                (order.getDeliveryOtpHash() != null && order.getDeliveryOtpHash().equalsIgnoreCase(calculatedHash));

        if (!match) {
            throw new BadRequestException("Incorrect delivery code. Ask the customer to open their order details and confirm the 6-digit code.");
        }

        Instant now = Instant.now();
        assignment.setStatus("delivered");
        assignment.setDeliveredAt(now);
        assignment.setDeliveryNote(note);
        deliveryAssignmentRepository.save(assignment);

        order.setOrderStatus("delivered");
        order.setDeliveredAt(now);
        orderRepository.save(order);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("orderId", order.getId());
        res.put("orderStatus", "delivered");
        res.put("paymentStatus", "paid");
        return res;
    }

    @Transactional(readOnly = true)
    public String getCustomerDeliveryCode(UUID orderId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getCustomerAuthId().equals(user.getId())) {
            throw new ForbiddenException("You can only access codes for your own orders.");
        }

        if (!"out_for_delivery".equalsIgnoreCase(order.getOrderStatus())) {
            throw new ConflictException("The delivery code is available when the order is out for delivery.");
        }

        DeliveryCode code = deliveryCodeRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery code not yet generated for order: " + orderId));

        return code.getCode();
    }

    @Transactional(readOnly = true)
    public List<DeliveryReview> getCustomerReviews() {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return deliveryReviewRepository.findByCustomerAuthIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public DeliveryReview reviewDelivery(UUID orderId, int rating, String reviewText) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getCustomerAuthId().equals(user.getId())) {
            throw new ForbiddenException("You can only review your own orders.");
        }

        if (!"delivered".equalsIgnoreCase(order.getOrderStatus())) {
            throw new ConflictException("You can review a delivery after it has been delivered.");
        }

        if (order.getDeliveryAgentId() == null) {
            throw new BadRequestException("No delivery agent was assigned to this order.");
        }

        if (deliveryReviewRepository.findByOrderId(orderId).isPresent()) {
            throw new ConflictException("This order has already been reviewed.");
        }

        DeliveryReview review = DeliveryReview.builder()
                .orderId(order.getId())
                .deliveryAgentId(order.getDeliveryAgentId())
                .customerAuthId(user.getId())
                .rating(rating)
                .review(reviewText != null ? reviewText.trim() : null)
                .build();
        review.setTenantId(order.getTenantId());

        return deliveryReviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminAgents(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
        List<DeliveryAgent> agents = deliveryAgentRepository.findByTenantIdOrderByFullNameAsc(tenant.getId());
        return Map.of("agents", agents, "requests", Collections.emptyList());
    }

    @Transactional
    public Map<String, Object> createDeliveryAgent(String fullName, String email, String phone, String address,
                                                  String vehicleType, String vehicleNumber, String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");

        if (fullName == null || fullName.isBlank() || email == null || email.isBlank() || phone == null || phone.isBlank()) {
            throw new BadRequestException("Name, email and phone number are required.");
        }

        String normalizedPhone = phone.replaceAll("\\D", "");
        if (normalizedPhone.length() > 10) {
            normalizedPhone = normalizedPhone.substring(normalizedPhone.length() - 10);
        }
        if (!normalizedPhone.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Enter a valid 10-digit Indian mobile number.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with email '" + normalizedEmail + "' already exists.");
        }

        String tempPassword = "Svt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "aA1!";

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(tempPassword))
                .fullName(fullName.trim())
                .phone(normalizedPhone)
                .role(RoleConstants.DELIVERY_AGENT)
                .tenantId(tenant.getId())
                .active(true)
                .build();
        user = userRepository.save(user);

        DeliveryAgent agent = DeliveryAgent.builder()
                .authUserId(user.getId())
                .fullName(fullName.trim())
                .email(normalizedEmail)
                .phone(normalizedPhone)
                .address(address != null ? address.trim() : null)
                .vehicleType(vehicleType != null ? vehicleType.trim() : null)
                .vehicleNumber(vehicleNumber != null ? vehicleNumber.trim() : null)
                .isActive(true)
                .availabilityStatus("offline")
                .build();
        agent.setTenantId(tenant.getId());
        agent = deliveryAgentRepository.save(agent);

        Map<String, Object> result = new HashMap<>();
        result.put("agent", agent);
        result.put("temporary_password", tempPassword);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerDeliveryPartner(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getDeliveryAgentId() == null || !"out_for_delivery".equalsIgnoreCase(order.getOrderStatus()) && !"delivered".equalsIgnoreCase(order.getOrderStatus())) {
            return Map.of("agent", Optional.empty());
        }

        DeliveryAgent agent = deliveryAgentRepository.findById(order.getDeliveryAgentId()).orElse(null);
        return Map.of("agent", agent != null ? agent : Optional.empty());
    }

    @Transactional
    public Map<String, Object> updateAgentProfile(String vehicleType, String vehicleNumber, String address, MultipartFile profilePhoto) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery agent account not found."));

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String path = storageService.store(profilePhoto, "agents/" + agent.getId() + "/profile");
            agent.setProfilePhotoPath(storageService.resolveUrl(path));
        }
        if (vehicleType != null && !vehicleType.isBlank()) {
            agent.setVehicleType(vehicleType.trim());
        }
        if (vehicleNumber != null && !vehicleNumber.isBlank()) {
            agent.setVehicleNumber(vehicleNumber.trim());
        }
        if (address != null && !address.isBlank()) {
            agent.setAddress(address.trim());
        }

        deliveryAgentRepository.save(agent);
        return Map.of("success", true, "agent", agent);
    }
}
