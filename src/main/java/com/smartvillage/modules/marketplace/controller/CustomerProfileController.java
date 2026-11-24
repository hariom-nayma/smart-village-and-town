package com.smartvillage.modules.marketplace.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.storage.StorageService;
import com.smartvillage.modules.marketplace.entity.CustomerProfile;
import com.smartvillage.modules.marketplace.entity.Order;
import com.smartvillage.modules.marketplace.repository.CustomerProfileRepository;
import com.smartvillage.modules.marketplace.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Customer Account & Profiles", description = "Customer address, profile photo, checkout details, and orders")
public class CustomerProfileController {

    private final CustomerProfileRepository customerProfileRepository;
    private final OrderRepository orderRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    @GetMapping("/customer-profile")
    @Operation(summary = "Get customer profile, address, and avatar")
    public ApiResponse<Map<String, Object>> getProfile() {
        UUID authUserId = SecurityUtils.getCurrentUserId();
        CustomerProfile profile = customerProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found."));

        Map<String, Object> addressMap = new HashMap<>();
        if (profile.getDefaultDeliveryAddress() != null && !profile.getDefaultDeliveryAddress().isBlank()) {
            try {
                addressMap = objectMapper.readValue(profile.getDefaultDeliveryAddress(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        }

        Map<String, Object> p = new HashMap<>();
        p.put("auth_user_id", profile.getAuthUserId());
        p.put("tenant_id", profile.getTenantId());
        p.put("full_name", profile.getFullName());
        p.put("phone", profile.getPhone());
        p.put("profile_photo_path", profile.getProfilePhotoPath());
        p.put("profile_photo_url", profile.getProfilePhotoPath() != null ? storageService.resolveUrl(profile.getProfilePhotoPath()) : null);
        p.put("address", addressMap);
        p.put("approval_status", profile.getApprovalStatus());
        p.put("active", profile.isActive());

        return ApiResponse.ok(Map.of("profile", p));
    }

    @PostMapping("/customer-profile")
    @Operation(summary = "Update customer address, name, or profile photo")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestParam(value = "full_name", required = false) String fullName,
                                                          @RequestParam(value = "address_line1", required = false) String line1,
                                                          @RequestParam(value = "area", required = false) String area,
                                                          @RequestParam(value = "city", required = false) String city,
                                                          @RequestParam(value = "pincode", required = false) String pincode,
                                                          @RequestParam(value = "profile_photo", required = false) MultipartFile profilePhoto) {
        UUID authUserId = SecurityUtils.getCurrentUserId();
        CustomerProfile profile = customerProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found."));

        if (fullName != null && !fullName.isBlank()) {
            profile.setFullName(fullName.trim());
        }

        Map<String, Object> addressMap = new HashMap<>();
        if (profile.getDefaultDeliveryAddress() != null && !profile.getDefaultDeliveryAddress().isBlank()) {
            try {
                addressMap = objectMapper.readValue(profile.getDefaultDeliveryAddress(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        }
        if (line1 != null) addressMap.put("line1", line1.trim());
        if (area != null) addressMap.put("area", area.trim());
        if (city != null) addressMap.put("city", city.trim());
        if (pincode != null) addressMap.put("pincode", pincode.trim());

        try {
            profile.setDefaultDeliveryAddress(objectMapper.writeValueAsString(addressMap));
        } catch (Exception ignored) {}

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String path = storageService.store(profilePhoto, "customers/" + profile.getId() + "/avatar");
            profile.setProfilePhotoPath(storageService.resolveUrl(path));
        }

        customerProfileRepository.save(profile);

        Map<String, Object> p = new HashMap<>();
        p.put("auth_user_id", profile.getAuthUserId());
        p.put("full_name", profile.getFullName());
        p.put("phone", profile.getPhone());
        p.put("profile_photo_url", profile.getProfilePhotoPath() != null ? storageService.resolveUrl(profile.getProfilePhotoPath()) : null);
        p.put("address", addressMap);
        p.put("approval_status", profile.getApprovalStatus());

        return ApiResponse.ok(Map.of("profile", p));
    }

    @GetMapping("/checkout-profile")
    @Operation(summary = "Get autofill customer checkout details")
    public ApiResponse<Map<String, Object>> getCheckoutProfile(@RequestParam(value = "tenant", required = false) String tenant) {
        UserPrincipal user = null;
        try {
            user = SecurityUtils.getCurrentUser();
        } catch (Exception ignored) {}

        if (user == null) {
            return ApiResponse.ok(Map.of("profile", Optional.empty()));
        }

        Optional<CustomerProfile> opt = customerProfileRepository.findByAuthUserId(user.getId());
        if (opt.isEmpty() || !opt.get().isActive()) {
            return ApiResponse.ok(Map.of("profile", Optional.empty()));
        }

        CustomerProfile p = opt.get();
        Map<String, Object> addressMap = new HashMap<>();
        if (p.getDefaultDeliveryAddress() != null && !p.getDefaultDeliveryAddress().isBlank()) {
            try {
                addressMap = objectMapper.readValue(p.getDefaultDeliveryAddress(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        }

        Map<String, Object> res = new HashMap<>(addressMap);
        res.put("full_name", p.getFullName());
        res.put("phone", p.getPhone());
        return ApiResponse.ok(Map.of("profile", res));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get order history for authenticated customer")
    public ApiResponse<Map<String, Object>> getMyOrders() {
        UUID authUserId = SecurityUtils.getCurrentUserId();
        List<Order> orders = orderRepository.findByCustomerAuthIdOrderByCreatedAtDesc(authUserId);
        return ApiResponse.ok(Map.of("orders", orders));
    }
}
