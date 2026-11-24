package com.smartvillage.modules.payment.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.modules.payment.entity.PaymentSubmission;
import com.smartvillage.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Payments & Proof Verification", description = "UPI payments, 12-digit UTR checks, screenshot uploads and reviews")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/account/payment")
    @Operation(summary = "Submit payment proof for an authenticated customer order")
    public ApiResponse<PaymentSubmission> submitPayment(@RequestParam("order_id") UUID orderId,
                                                        @RequestParam(value = "utr_number", required = false) String utrNumber,
                                                        @RequestParam(value = "proof", required = false) MultipartFile proof) {
        UUID customerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok("Payment proof submitted", paymentService.submitPayment(orderId, utrNumber, proof, customerId));
    }

    @PostMapping("/public/payment-submission")
    @Operation(summary = "Submit payment proof publicly using order reference & customer phone")
    public ApiResponse<PaymentSubmission> publicPaymentSubmission(@RequestParam("order_id") String orderId,
                                                                 @RequestParam("phone") String phone,
                                                                 @RequestParam(value = "utr_number", required = false) String utrNumber,
                                                                 @RequestParam(value = "proof", required = false) MultipartFile proof) {
        return ApiResponse.ok("Payment proof submitted", paymentService.publicSubmitPayment(orderId, phone, utrNumber, proof));
    }

    @PatchMapping("/vendor/payments/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Review payment submission (verified / rejected)")
    public ApiResponse<PaymentSubmission> reviewPayment(@PathVariable UUID id,
                                                        @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String note = body.get("note");
        return ApiResponse.ok("Payment status updated", paymentService.reviewPayment(id, status, note));
    }
}
