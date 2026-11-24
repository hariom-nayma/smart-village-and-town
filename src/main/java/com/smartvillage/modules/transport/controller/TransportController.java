package com.smartvillage.modules.transport.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.transport.dto.CreateTransportBookingRequest;
import com.smartvillage.modules.transport.entity.TransportBooking;
import com.smartvillage.modules.transport.entity.TransportRate;
import com.smartvillage.modules.transport.entity.TransportVehicle;
import com.smartvillage.modules.transport.service.TransportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Transport & Cargo Logistics", description = "Passenger & cargo vehicles, per-km rates, and trip bookings")
public class TransportController {

    private final TransportService transportService;

    @GetMapping("/transport/catalog")
    @Operation(summary = "Get approved transport vehicles, drivers, and rates catalog")
    public ApiResponse<Map<String, Object>> getCatalog(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(transportService.getCatalog(tenant));
    }

    @GetMapping("/transport/vehicles")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Get registered vehicles for current delivery driver")
    public ApiResponse<List<TransportVehicle>> getMyVehicles() {
        return ApiResponse.ok(transportService.getMyVehicles());
    }

    @PostMapping("/transport/vehicles")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Register a vehicle with RC, license, and photo documents")
    public ApiResponse<TransportVehicle> registerVehicle(@RequestParam("vehicle_type") String vehicleType,
                                                         @RequestParam("make_model") String makeModel,
                                                         @RequestParam("registration_number") String registrationNumber,
                                                         @RequestParam(value = "passenger_capacity", defaultValue = "0") int passengerCapacity,
                                                         @RequestParam(value = "cargo_weight_kg", required = false) BigDecimal cargoWeightKg,
                                                         @RequestParam(value = "cargo_area_sqft", required = false) BigDecimal cargoAreaSqft,
                                                         @RequestParam(value = "supports_passengers", defaultValue = "true") boolean supportsPassengers,
                                                         @RequestParam(value = "supports_goods", defaultValue = "false") boolean supportsGoods,
                                                         @RequestParam(value = "vehicle_photo", required = false) MultipartFile vehiclePhoto,
                                                         @RequestParam(value = "rc_document", required = false) MultipartFile rcDoc,
                                                         @RequestParam(value = "driving_licence", required = false) MultipartFile licenseDoc) {
        return ApiResponse.ok("Vehicle submitted for admin approval",
                transportService.registerVehicle(vehicleType, makeModel, registrationNumber, passengerCapacity,
                        cargoWeightKg, cargoAreaSqft, supportsPassengers, supportsGoods, vehiclePhoto, rcDoc, licenseDoc));
    }

    @PostMapping("/transport/rates")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Configure per-km rate, minimum fare, and waiting charge for vehicle")
    public ApiResponse<TransportRate> saveRate(@RequestBody Map<String, Object> body) {
        UUID vehicleId = UUID.fromString((String) body.get("vehicle_id"));
        String serviceType = (String) body.get("service_type");
        BigDecimal ratePerKm = new BigDecimal(body.get("rate_per_km").toString());
        BigDecimal minimumFare = body.get("minimum_fare") != null ? new BigDecimal(body.get("minimum_fare").toString()) : BigDecimal.ZERO;
        BigDecimal waitingRate = body.get("waiting_rate_per_hour") != null ? new BigDecimal(body.get("waiting_rate_per_hour").toString()) : BigDecimal.ZERO;

        return ApiResponse.ok("Rate saved successfully",
                transportService.saveRate(vehicleId, serviceType, ratePerKm, minimumFare, waitingRate));
    }

    @PostMapping("/transport/bookings")
    @Operation(summary = "Book transport trip (passenger or cargo)")
    public ApiResponse<TransportBooking> createBooking(@Valid @RequestBody CreateTransportBookingRequest request) {
        return ApiResponse.ok("Transport booking requested", transportService.createBooking(request));
    }

    @GetMapping("/transport/bookings")
    @Operation(summary = "Get bookings for the authenticated customer")
    public ApiResponse<List<TransportBooking>> getCustomerBookings() {
        return ApiResponse.ok(transportService.getCustomerBookings());
    }

    @GetMapping("/transport/driver/bookings")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Get bookings assigned to the driver")
    public ApiResponse<List<TransportBooking>> getDriverBookings() {
        return ApiResponse.ok(transportService.getDriverBookings());
    }

    @PatchMapping("/transport/driver/bookings/{id}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Driver trip action (accepted, driver_arriving, in_progress, completed, rejected)")
    public ApiResponse<TransportBooking> driverBookingAction(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Trip status updated", transportService.driverBookingAction(id, body.get("booking_status")));
    }

    @GetMapping("/admin/transport")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Admin view of all transport vehicles and bookings for tenant")
    public ApiResponse<Map<String, Object>> adminTransport(@RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok(transportService.adminTransportList(tenant));
    }

    @PatchMapping("/admin/transport/vehicles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Admin review of vehicle registration (approved / rejected / suspended)")
    public ApiResponse<TransportVehicle> adminReviewVehicle(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Vehicle status updated",
                transportService.adminReviewVehicle(id, body.get("approval_status"), body.get("admin_note")));
    }
}
