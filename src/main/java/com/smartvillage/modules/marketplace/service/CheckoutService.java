package com.smartvillage.modules.marketplace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartvillage.common.constant.RoleConstants;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.util.CodeGenerator;
import com.smartvillage.modules.auth.entity.User;
import com.smartvillage.modules.auth.repository.UserRepository;
import com.smartvillage.modules.marketplace.dto.*;
import com.smartvillage.modules.marketplace.entity.*;
import com.smartvillage.modules.marketplace.repository.*;
import com.smartvillage.modules.tenant.entity.Tenant;
import com.smartvillage.modules.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final TenantService tenantService;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Transactional
    public CheckoutResultDto checkout(CheckoutRequest request) {
        String tenantSlug = request.getTenant() != null ? request.getTenant() : "gursarai";
        Tenant tenant = tenantService.getTenantEntityBySlug(tenantSlug);

        CustomerInfoDto customerInfo = request.getCustomer();
        String mobile = customerInfo.getMobile().replaceAll("\\D", "");
        if (mobile.length() > 10) {
            mobile = mobile.substring(mobile.length() - 10);
        }

        // Group cart items by vendor ID
        Map<UUID, List<CartItemDto>> vendorGroups = request.getItems().stream()
                .collect(Collectors.groupingBy(CartItemDto::getVendorId));

        // Validate vendors and payment details
        String paymentMethod = customerInfo.getPaymentMethod() != null ? customerInfo.getPaymentMethod().toLowerCase() : "upi";
        for (UUID vendorId : vendorGroups.keySet()) {
            Vendor vendor = vendorRepository.findByIdAndTenantId(vendorId, tenant.getId())
                    .orElseThrow(() -> new BadRequestException("One of the selected stores is not available in this marketplace."));

            if (!vendor.isActive() || !"approved".equalsIgnoreCase(vendor.getStatus())) {
                throw new BadRequestException("Store '" + vendor.getBusinessName() + "' is currently unable to accept orders.");
            }

            if ("upi".equals(paymentMethod) && (vendor.getUpiId() == null || vendor.getUpiId().isBlank()) && (vendor.getUpiQrUrl() == null || vendor.getUpiQrUrl().isBlank())) {
                throw new ConflictException("Store '" + vendor.getBusinessName() + "' has not configured UPI payment details yet. Choose Cash on Delivery or contact the store.");
            }
        }

        // Resolve or create customer account
        UUID authUserId = null;
        boolean approvalRequired = false;
        String customerStatus = "approved";

        UserPrincipal currentUser = null;
        try {
            currentUser = SecurityUtils.getCurrentUser();
        } catch (Exception ignored) {}

        if (currentUser != null) {
            authUserId = currentUser.getId();
            CustomerProfile profile = customerProfileRepository.findByAuthUserId(authUserId)
                    .orElseThrow(() -> new ForbiddenException("Customer profile not found."));

            if (!profile.isActive()) {
                throw new ForbiddenException("Your account is currently deactivated. Please contact the marketplace admin.");
            }
            if (!"approved".equalsIgnoreCase(profile.getApprovalStatus())) {
                throw new ForbiddenException("Your customer account is not approved yet.");
            }
        } else {
            // Guest checkout
            Optional<CustomerProfile> existingProfile = customerProfileRepository.findByTenantIdAndPhone(tenant.getId(), mobile);
            if (existingProfile.isPresent()) {
                CustomerProfile p = existingProfile.get();
                if ("pending".equalsIgnoreCase(p.getApprovalStatus())) {
                    throw new ConflictException("An approval request for this mobile number is already pending. The admin will call you after review.");
                }
                if ("rejected".equalsIgnoreCase(p.getApprovalStatus())) {
                    throw new ForbiddenException("This customer request was not approved. Please contact the marketplace admin.");
                }
                throw new ConflictException("This mobile number already has an approved account. Please log in with your mobile number and PIN before placing another order.");
            }

            // Create temporary user credentials
            String tempPassword = CodeGenerator.generateCustomerPin();
            String loginEmail = "customer-" + mobile + "@marketplace.invalid";

            User newUser = User.builder()
                    .email(loginEmail)
                    .password(passwordEncoder.encode(tempPassword))
                    .fullName(customerInfo.getFullName().trim())
                    .phone(mobile)
                    .role(RoleConstants.CUSTOMER)
                    .tenantId(tenant.getId())
                    .active(true)
                    .build();
            newUser = userRepository.save(newUser);
            authUserId = newUser.getId();

            CustomerProfile newProfile = CustomerProfile.builder()
                    .authUserId(authUserId)
                    .tenantId(tenant.getId())
                    .fullName(customerInfo.getFullName().trim())
                    .email(loginEmail)
                    .phone(mobile)
                    .approvalStatus("pending")
                    .active(true)
                    .build();
            customerProfileRepository.save(newProfile);

            approvalRequired = true;
            customerStatus = "pending";
        }

        // Process orders for each vendor
        List<OrderDto> createdOrders = new ArrayList<>();
        String deliveryAddressJson = serializeAddress(customerInfo);

        for (Map.Entry<UUID, List<CartItemDto>> entry : vendorGroups.entrySet()) {
            UUID vendorId = entry.getKey();
            List<CartItemDto> cartItems = entry.getValue();
            Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();

            BigDecimal subtotal = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItemDto item : cartItems) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new BadRequestException("Product not found: " + item.getProductId()));

                if (!product.isActive()) {
                    throw new BadRequestException("Product '" + product.getName() + "' is no longer available.");
                }

                if (product.getStockQuantity().compareTo(item.getQuantity()) < 0) {
                    throw new BadRequestException("Insufficient stock for product: " + product.getName());
                }

                // Decrement stock
                product.setStockQuantity(product.getStockQuantity().subtract(item.getQuantity()));
                productRepository.save(product);

                BigDecimal lineTotal = product.getPrice().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(lineTotal);

                OrderItem orderItem = OrderItem.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .unit(product.getUnit())
                        .quantity(item.getQuantity())
                        .unitPrice(product.getPrice())
                        .lineTotal(lineTotal)
                        .build();
                orderItems.add(orderItem);
            }

            // Check minimum order amount
            if (vendor.getMinOrderAmount() != null && subtotal.compareTo(vendor.getMinOrderAmount()) < 0) {
                throw new BadRequestException("Minimum order for store '" + vendor.getBusinessName() + "' is ₹" + vendor.getMinOrderAmount() + ". Add more items to continue.");
            }

            BigDecimal deliveryCharge = "delivery".equalsIgnoreCase(request.getFulfillmentType()) ? vendor.getDeliveryCharge() : BigDecimal.ZERO;
            BigDecimal prepaidDiscount = BigDecimal.ZERO;
            BigDecimal total = subtotal.add(deliveryCharge);

            if ("upi".equals(paymentMethod)) {
                // 5% upfront discount on subtotal
                prepaidDiscount = subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
                total = total.subtract(prepaidDiscount);
                if (total.compareTo(BigDecimal.ZERO) < 0) {
                    total = BigDecimal.ZERO;
                }
            }

            String orderNumber = CodeGenerator.generateOrderNumber();

            Order order = Order.builder()
                    .vendorId(vendor.getId())
                    .customerAuthId(authUserId)
                    .orderNumber(orderNumber)
                    .customerName(customerInfo.getFullName().trim())
                    .customerPhone(mobile)
                    .deliveryAddress(deliveryAddressJson)
                    .deliveryMethod(request.getFulfillmentType() != null ? request.getFulfillmentType() : "delivery")
                    .paymentMethod(paymentMethod)
                    .paymentStatus("pending")
                    .paymentReviewStatus("upi".equals(paymentMethod) ? "pending" : "not_required")
                    .orderStatus("placed")
                    .subtotal(subtotal)
                    .deliveryCharge(deliveryCharge)
                    .prepaidDiscount(prepaidDiscount)
                    .total(total)
                    .notes(request.getNotes())
                    .build();
            order.setTenantId(tenant.getId());

            for (OrderItem oi : orderItems) {
                oi.setOrder(order);
            }
            order.setItems(orderItems);

            order = orderRepository.save(order);

            // Construct UPI Intent URL if paymentMethod is UPI
            String upiIntentUrl = null;
            if ("upi".equals(paymentMethod) && vendor.getUpiId() != null) {
                String pa = URLEncoder.encode(vendor.getUpiId(), StandardCharsets.UTF_8);
                String pn = URLEncoder.encode(vendor.getBusinessName(), StandardCharsets.UTF_8);
                String am = URLEncoder.encode(total.setScale(2, RoundingMode.HALF_UP).toString(), StandardCharsets.UTF_8);
                String tr = URLEncoder.encode(orderNumber, StandardCharsets.UTF_8);
                String tn = URLEncoder.encode("Marketplace order " + orderNumber, StandardCharsets.UTF_8);
                upiIntentUrl = String.format("upi://pay?pa=%s&pn=%s&am=%s&tr=%s&tn=%s&cu=INR", pa, pn, am, tr, tn);
            }

            OrderDto dto = OrderDto.builder()
                    .id(order.getId())
                    .tenantId(order.getTenantId())
                    .vendorId(vendor.getId())
                    .vendorName(vendor.getBusinessName())
                    .vendorUpiId(vendor.getUpiId())
                    .vendorQrUrl(vendor.getUpiQrUrl())
                    .upiIntentUrl(upiIntentUrl)
                    .paymentRequired("upi".equals(paymentMethod))
                    .orderNumber(order.getOrderNumber())
                    .customerName(order.getCustomerName())
                    .customerPhone(order.getCustomerPhone())
                    .deliveryMethod(order.getDeliveryMethod())
                    .paymentMethod(order.getPaymentMethod())
                    .paymentStatus(order.getPaymentStatus())
                    .paymentReviewStatus(order.getPaymentReviewStatus())
                    .orderStatus(order.getOrderStatus())
                    .subtotal(order.getSubtotal())
                    .deliveryCharge(order.getDeliveryCharge())
                    .prepaidDiscount(order.getPrepaidDiscount())
                    .total(order.getTotal())
                    .notes(order.getNotes())
                    .createdAt(order.getCreatedAt())
                    .items(orderItems.stream().map(oi -> OrderItemDto.builder()
                            .id(oi.getId())
                            .productId(oi.getProductId())
                            .productName(oi.getProductName())
                            .unit(oi.getUnit())
                            .quantity(oi.getQuantity())
                            .unitPrice(oi.getUnitPrice())
                            .lineTotal(oi.getLineTotal())
                            .build()).toList())
                    .build();

            createdOrders.add(dto);
        }

        return CheckoutResultDto.builder()
                .success(true)
                .approvalRequired(approvalRequired)
                .customerStatus(customerStatus)
                .orders(createdOrders)
                .build();
    }

    private String serializeAddress(CustomerInfoDto c) {
        Map<String, Object> map = new HashMap<>();
        map.put("line1", c.getAddressLine1() != null ? c.getAddressLine1() : "");
        map.put("line2", c.getAddressLine2() != null ? c.getAddressLine2() : "");
        map.put("area", c.getArea() != null ? c.getArea() : "");
        map.put("city", c.getCity() != null ? c.getCity() : "Gursarai");
        map.put("pincode", c.getPincode() != null ? c.getPincode() : "");
        map.put("recipient_name", c.getFullName());
        map.put("phone", c.getMobile());
        map.put("latitude", c.getLatitude());
        map.put("longitude", c.getLongitude());
        map.put("location_label", c.getLocationLabel());
        if (c.getLatitude() != null && c.getLongitude() != null) {
            map.put("map_url", "https://www.google.com/maps?q=" + c.getLatitude() + "," + c.getLongitude());
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
