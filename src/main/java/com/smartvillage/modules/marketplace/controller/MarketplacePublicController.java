package com.smartvillage.modules.marketplace.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.marketplace.dto.CheckoutRequest;
import com.smartvillage.modules.marketplace.dto.CheckoutResultDto;
import com.smartvillage.modules.marketplace.entity.Category;
import com.smartvillage.modules.marketplace.service.CheckoutService;
import com.smartvillage.modules.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Marketplace Public", description = "Public storefront, checkout, and vendor registration bootstrap")
public class MarketplacePublicController {

    private final MarketplaceService marketplaceService;
    private final CheckoutService checkoutService;

    @GetMapping("/public/vendor-registration-data")
    @Operation(summary = "Bootstrap data for vendor onboarding registration form")
    public ApiResponse<Map<String, Object>> getVendorRegistrationData(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(marketplaceService.getPublicVendorRegistrationData(tenant));
    }

    @GetMapping("/marketplace/categories")
    @Operation(summary = "Get active categories for storefront")
    public ApiResponse<List<Category>> getActiveCategories(@RequestParam(required = false) String tenant) {
        return ApiResponse.ok(marketplaceService.getActiveCategories(tenant));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Multi-vendor atomic cart checkout with UPI/COD processing")
    public ApiResponse<CheckoutResultDto> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.ok("Checkout completed successfully", checkoutService.checkout(request));
    }
}
