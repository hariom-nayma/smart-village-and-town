package com.smartvillage.modules.servicebooking.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.servicebooking.entity.ServiceBooking;
import com.smartvillage.modules.servicebooking.service.ServiceBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/service-bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
@Tag(name = "Vendor Service Bookings", description = "Service provider booking management, verification, and analytics")
public class VendorServiceBookingController {

    private final ServiceBookingService bookingService;

    @GetMapping
    @Operation(summary = "Get all service bookings for vendor store")
    public ApiResponse<List<ServiceBooking>> getVendorBookings() {
        return ApiResponse.ok(bookingService.getVendorBookings());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update booking status (accepted, in_progress, awaiting_customer_confirmation)")
    public ApiResponse<Void> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        bookingService.updateVendorBookingStatus(id, body.get("status"));
        return ApiResponse.ok("Booking status updated", null);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete booking by verifying 6-digit customer completion code")
    public ApiResponse<Void> completeBooking(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        bookingService.completeBooking(id, body.get("completion_code"));
        return ApiResponse.ok("Service booking marked completed", null);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get daily, weekly, monthly, and yearly service analytics")
    public ApiResponse<Map<String, Object>> getAnalytics() {
        return ApiResponse.ok(bookingService.getVendorAnalytics());
    }
}
