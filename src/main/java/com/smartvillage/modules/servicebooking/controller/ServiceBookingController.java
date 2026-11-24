package com.smartvillage.modules.servicebooking.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.servicebooking.dto.CreateServiceBookingRequest;
import com.smartvillage.modules.servicebooking.entity.ServiceBooking;
import com.smartvillage.modules.servicebooking.entity.ServicePaymentSubmission;
import com.smartvillage.modules.servicebooking.entity.ServiceReview;
import com.smartvillage.modules.servicebooking.service.ServiceBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Customer Service Bookings", description = "Local services booking, upfront fees, completion codes and cancellation")
public class ServiceBookingController {

    private final ServiceBookingService bookingService;

    @PostMapping("/bookings")
    @Operation(summary = "Create a new service appointment / home visit booking")
    public ApiResponse<Map<String, Object>> createBooking(@Valid @RequestBody CreateServiceBookingRequest request) {
        return ApiResponse.ok("Service booking created", bookingService.createBooking(request));
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get list of service bookings for the authenticated customer")
    public ApiResponse<Map<String, Object>> getMyBookings() {
        return ApiResponse.ok(Map.of("bookings", bookingService.getCustomerBookings()));
    }

    @PostMapping("/bookings/{id}/payment")
    @Operation(summary = "Submit upfront visiting/booking charge payment proof")
    public ApiResponse<ServicePaymentSubmission> submitPayment(@PathVariable UUID id,
                                                               @RequestParam(value = "utr_number", required = false) String utrNumber,
                                                               @RequestParam(value = "proof", required = false) MultipartFile proof) {
        return ApiResponse.ok("Service payment submitted", bookingService.submitUpfrontPayment(id, utrNumber, proof));
    }

    @PostMapping("/bookings/{id}/review")
    @Operation(summary = "Submit service rating and customer review")
    public ApiResponse<ServiceReview> reviewBooking(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        int rating = ((Number) body.get("rating")).intValue();
        String review = (String) body.get("review");
        return ApiResponse.ok("Service review submitted", bookingService.reviewBooking(id, rating, review));
    }

    @PostMapping("/bookings/{id}/cancel")
    @Operation(summary = "Cancel booking with 12-hour advance policy and request advance refund")
    public ApiResponse<Void> cancelBooking(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Customer cancelled";
        bookingService.cancelBookingByCustomer(id, reason);
        return ApiResponse.ok("Booking cancelled successfully", null);
    }
}
