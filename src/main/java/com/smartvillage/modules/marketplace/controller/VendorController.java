package com.smartvillage.modules.marketplace.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.marketplace.entity.CategoryRequest;
import com.smartvillage.modules.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
@Tag(name = "Vendor Operations", description = "Vendor Center endpoints for orders, QR codes, category requests")
public class VendorController {

    private final MarketplaceService marketplaceService;

    @GetMapping({"/dashboard", "/dashboard-v2"})
    @Operation(summary = "Get vendor profile, live orders, and payment records")
    public ApiResponse<Map<String, Object>> getDashboard() {
        return ApiResponse.ok(marketplaceService.getVendorDashboard());
    }

    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Update order preparation status (accepted, processing, ready, cancelled)")
    public ApiResponse<Void> updateOrderStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        marketplaceService.updateVendorOrderStatus(id, body.get("status"));
        return ApiResponse.ok("Order status updated successfully", null);
    }

    @PostMapping("/qr")
    @Operation(summary = "Upload UPI QR code image or update UPI ID")
    public ApiResponse<Map<String, String>> uploadQr(@RequestParam(value = "file", required = false) MultipartFile file,
                                                     @RequestParam(value = "upi_id", required = false) String upiId) {
        return ApiResponse.ok("UPI settings updated", marketplaceService.updateVendorQr(file, upiId));
    }

    @DeleteMapping("/qr")
    @Operation(summary = "Remove configured UPI QR code")
    public ApiResponse<Void> removeQr() {
        marketplaceService.removeVendorQr();
        return ApiResponse.ok("UPI QR removed", null);
    }

    @PostMapping("/category-requests")
    @Operation(summary = "Submit a new category request to marketplace admin")
    public ApiResponse<CategoryRequest> requestCategory(@RequestBody Map<String, String> body) {
        return ApiResponse.ok("Category request submitted",
                marketplaceService.requestCategory(body.get("requested_name"), body.get("reason")));
    }
}
