package com.smartvillage.modules.delivery.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.delivery.entity.DeliveryReview;
import com.smartvillage.modules.delivery.service.DeliveryService;
import com.smartvillage.modules.marketplace.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Delivery Partner Operations", description = "Endpoints for delivery agents, assignments, OTP verification, and reviews")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/delivery/me")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Get delivery partner profile, assigned jobs, and ratings")
    public ApiResponse<Map<String, Object>> getMe() {
        return ApiResponse.ok(deliveryService.getAgentMe());
    }

    @GetMapping("/delivery/available-orders")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Get unclaimed orders ready for delivery in the driver's village/tenant")
    public ApiResponse<List<Map<String, Object>>> getAvailableOrders() {
        return ApiResponse.ok(deliveryService.getAvailableOrders());
    }

    @PostMapping("/delivery/orders/{orderId}/claim")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Claim an available order (First-to-Claim gig dispatch)")
    public ApiResponse<Map<String, Object>> claimOrder(@PathVariable UUID orderId) {
        return ApiResponse.ok(deliveryService.claimOrder(orderId));
    }

    @PostMapping("/delivery/claim")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Claim an available order by ID in request body")
    public ApiResponse<Map<String, Object>> claimOrderByBody(@RequestBody Map<String, UUID> body) {
        UUID orderId = body.get("order_id");
        return ApiResponse.ok(deliveryService.claimOrder(orderId));
    }

    @PatchMapping("/delivery/status")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Update driver availability status (offline, available, busy)")
    public ApiResponse<Void> updateStatus(@RequestBody Map<String, String> body) {
        deliveryService.updateAgentAvailability(body.get("status"));
        return ApiResponse.ok("Availability updated", null);
    }

    @PatchMapping("/delivery/assignments/{id}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Advance assignment status (accepted, picked_up, out_for_delivery)")
    public ApiResponse<Map<String, Object>> updateAssignment(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(deliveryService.updateAssignmentStatus(id, body.get("status")));
    }

    @PostMapping("/delivery/payment/{orderId}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Collect cash on delivery (COD) payment from customer")
    public ApiResponse<Order> collectPayment(@PathVariable UUID orderId) {
        return ApiResponse.ok("Payment recorded as collected", deliveryService.recordCashPayment(orderId));
    }

    @PostMapping("/delivery/complete/{assignmentId}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Complete delivery using 6-digit customer delivery OTP")
    public ApiResponse<Map<String, Object>> completeDelivery(@PathVariable UUID assignmentId,
                                                             @RequestParam String otp,
                                                             @RequestParam(required = false) String note) {
        return ApiResponse.ok("Delivery completed", deliveryService.completeDelivery(assignmentId, otp, note));
    }

    @PostMapping("/delivery/profile")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Update delivery partner profile photo or vehicle details")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestParam(value = "vehicle_type", required = false) String vehicleType,
                                                          @RequestParam(value = "vehicle_number", required = false) String vehicleNumber,
                                                          @RequestParam(value = "address", required = false) String address,
                                                          @RequestParam(value = "profile_photo", required = false) MultipartFile profilePhoto) {
        return ApiResponse.ok(deliveryService.updateAgentProfile(vehicleType, vehicleNumber, address, profilePhoto));
    }

    @GetMapping("/account/orders/{orderId}/delivery-partner")
    @Operation(summary = "Get delivery partner assigned to an out-for-delivery order (Customer)")
    public ApiResponse<Map<String, Object>> getDeliveryPartner(@PathVariable UUID orderId) {
        return ApiResponse.ok(deliveryService.getCustomerDeliveryPartner(orderId));
    }

    @GetMapping("/account/orders/{orderId}/delivery-code")
    @Operation(summary = "Get 6-digit delivery OTP for an out-for-delivery order (Customer)")
    public ApiResponse<Map<String, String>> getDeliveryCode(@PathVariable UUID orderId) {
        return ApiResponse.ok(Map.of("code", deliveryService.getCustomerDeliveryCode(orderId)));
    }

    @GetMapping("/account/orders/reviews")
    @Operation(summary = "Get delivery reviews submitted by the authenticated customer")
    public ApiResponse<Map<String, Object>> getCustomerReviews() {
        return ApiResponse.ok(Map.of("reviews", deliveryService.getCustomerReviews()));
    }

    @PostMapping("/account/orders/{orderId}/review")
    @Operation(summary = "Submit delivery agent review and rating (Customer)")
    public ApiResponse<DeliveryReview> reviewDelivery(@PathVariable UUID orderId,
                                                      @RequestBody Map<String, Object> body) {
        int rating = ((Number) body.get("rating")).intValue();
        String reviewText = (String) body.get("review_text");
        return ApiResponse.ok("Review submitted", deliveryService.reviewDelivery(orderId, rating, reviewText));
    }
}
