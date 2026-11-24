package com.smartvillage.modules.marketplace.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.marketplace.entity.Category;
import com.smartvillage.modules.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin Marketplace", description = "Administration of marketplace categories, customer approvals")
public class AdminMarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/categories")
    @Operation(summary = "List categories and category requests for admin operations")
    public ApiResponse<Map<String, Object>> getCategories(@RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok(marketplaceService.getAdminCategories(tenant));
    }

    @GetMapping("/orders")
    @Operation(summary = "List all marketplace orders and active agents for admin operations")
    public ApiResponse<Map<String, Object>> getOrders(@RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok(marketplaceService.getAdminOrders(tenant));
    }

    @GetMapping("/payments")
    @Operation(summary = "List all customer payment submissions and vendor configurations")
    public ApiResponse<Map<String, Object>> getPayments(@RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok(marketplaceService.getAdminPayments(tenant));
    }

    @GetMapping("/customer-requests")
    @Operation(summary = "List all pending customer approval requests with order history")
    public ApiResponse<Map<String, Object>> getCustomerRequests(@RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok(marketplaceService.getAdminCustomerRequests(tenant));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new product/service category")
    public ApiResponse<Category> createCategory(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String nameHi = (String) body.get("name_hi");
        String icon = (String) body.get("icon");
        String description = (String) body.get("description");
        Integer sortOrder = body.get("sort_order") != null ? ((Number) body.get("sort_order")).intValue() : 100;
        String tenantSlug = (String) body.get("tenant");

        return ApiResponse.ok("Category created",
                marketplaceService.createCategory(name, nameHi, icon, description, sortOrder, tenantSlug));
    }

    @PatchMapping("/category-requests/{id}")
    @Operation(summary = "Review vendor category request (approve / reject)")
    public ApiResponse<Map<String, Object>> reviewCategoryRequest(@PathVariable UUID id,
                                                                  @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Category request reviewed",
                marketplaceService.reviewCategoryRequest(id, body.get("status"), body.get("review_note")));
    }

    @PatchMapping({"/customer-requests/{id}", "/customer-requests/{id}/review"})
    @Operation(summary = "Review customer registration request (approves and generates one-time PIN)")
    public ApiResponse<Map<String, Object>> reviewCustomer(@PathVariable UUID id,
                                                           @RequestBody Map<String, String> body) {
        return ApiResponse.ok(marketplaceService.reviewCustomerRequest(id, body.get("status")));
    }
}
