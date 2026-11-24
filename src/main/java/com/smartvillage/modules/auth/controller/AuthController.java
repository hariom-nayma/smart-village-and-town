package com.smartvillage.modules.auth.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.auth.dto.*;
import com.smartvillage.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Authentication & User Management", description = "Endpoints for login, registration, and user accounts")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    @Operation(summary = "Authenticate user with email/phone and password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/auth/otp/send")
    @Operation(summary = "Send 6-digit OTP to mobile number for login")
    public ApiResponse<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return ApiResponse.ok("OTP dispatched successfully", authService.sendLoginOtp(request));
    }

    @PostMapping("/auth/otp/verify")
    @Operation(summary = "Verify OTP code and authenticate / auto-register user")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpLoginRequest request) {
        return ApiResponse.ok("OTP verified successfully. Login granted.", authService.verifyOtpLogin(request));
    }

    @PostMapping("/auth/customer-pin-login")
    @Operation(summary = "Customer login using mobile number and 8-digit PIN")
    public ApiResponse<AuthResponse> customerPinLogin(@Valid @RequestBody CustomerPinLoginRequest request) {
        return ApiResponse.ok("Customer PIN login successful", authService.customerPinLogin(request));
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Register a new user account")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Registration successful", authService.register(request));
    }

    @GetMapping("/auth/me")
    @Operation(summary = "Get current authenticated user profile")
    public ApiResponse<UserDto> getMe() {
        return ApiResponse.ok(authService.getCurrentUserDto());
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List users for a tenant (Admin / Super Admin)")
    public ApiResponse<Map<String, Object>> listUsers(@RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok(authService.getAdminUsers(tenant));
    }

    @PatchMapping({"/admin/users/{id}", "/admin/users/{id}/status"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Activate or deactivate an account (customer, vendor, delivery, admin)")
    public ApiResponse<Void> updateUserStatus(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        boolean active = Boolean.TRUE.equals(body.get("active"));
        String tenant = (String) body.get("tenant");
        authService.updateAdminUserStatus(id, type, active, tenant);
        return ApiResponse.ok("Account status updated successfully", null);
    }
}
