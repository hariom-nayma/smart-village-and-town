package com.smartvillage.modules.tenant.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.tenant.dto.TenantDto;
import com.smartvillage.modules.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Multi-Tenant Local Body Management (Village & Town)")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/public/tenants")
    @Operation(summary = "Get list of all active local village/town marketplaces")
    public ApiResponse<List<TenantDto>> getActiveTenants() {
        return ApiResponse.ok(tenantService.getActiveTenants());
    }

    @GetMapping("/public/tenants/{slug}")
    @Operation(summary = "Get tenant details by slug")
    public ApiResponse<TenantDto> getTenantBySlug(@PathVariable String slug) {
        return ApiResponse.ok(tenantService.getTenantBySlug(slug));
    }

    @GetMapping("/public/tenants/constituency/{constituency}")
    @Operation(summary = "Get all towns within an assembly constituency")
    public ApiResponse<List<TenantDto>> getTenantsByConstituency(@PathVariable String constituency) {
        return ApiResponse.ok(tenantService.getTenantsByConstituency(constituency));
    }

    @PostMapping("/admin/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new village/town tenant (Super Admin only)")
    public ApiResponse<TenantDto> createTenant(@RequestBody TenantDto request) {
        return ApiResponse.ok("Tenant created successfully", tenantService.createTenant(request));
    }
}
