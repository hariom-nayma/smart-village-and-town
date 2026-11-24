package com.smartvillage.modules.payment.service;

import com.smartvillage.common.audit.AuditService;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.storage.StorageService;
import com.smartvillage.modules.marketplace.entity.Order;
import com.smartvillage.modules.marketplace.repository.OrderRepository;
import com.smartvillage.modules.payment.entity.PaymentSubmission;
import com.smartvillage.modules.payment.repository.PaymentSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentSubmissionRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    @Transactional
    public PaymentSubmission submitPayment(UUID orderId, String utr, MultipartFile proofFile, UUID customerAuthId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (customerAuthId != null && !order.getCustomerAuthId().equals(customerAuthId)) {
            throw new ForbiddenException("You can only submit payment proofs for your own orders.");
        }

        return processPaymentSubmission(order, utr, proofFile, customerAuthId);
    }

    @Transactional
    public PaymentSubmission publicSubmitPayment(String orderReference, String phone, String utr, MultipartFile proofFile) {
        String cleanPhone = phone.replaceAll("\\D", "");
        if (cleanPhone.length() > 10) cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);

        Order order = orderRepository.findByOrderNumber(orderReference.trim())
                .or(() -> {
                    try {
                        return orderRepository.findById(UUID.fromString(orderReference.trim()));
                    } catch (Exception e) {
                        return java.util.Optional.empty();
                    }
                })
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with reference: " + orderReference));

        if (!order.getCustomerPhone().endsWith(cleanPhone)) {
            throw new BadRequestException("Mobile number does not match order record.");
        }

        return processPaymentSubmission(order, utr, proofFile, order.getCustomerAuthId());
    }

    private PaymentSubmission processPaymentSubmission(Order order, String utr, MultipartFile proofFile, UUID customerId) {
        if ("paid".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new ConflictException("This order is already marked as Paid.");
        }

        String cleanedUtr = utr != null ? utr.replaceAll("\\s", "") : null;
        boolean hasFile = proofFile != null && !proofFile.isEmpty();

        if ((cleanedUtr == null || cleanedUtr.isBlank()) && !hasFile) {
            throw new BadRequestException("Enter the 12-digit UTR or upload a payment screenshot.");
        }

        if (cleanedUtr != null && !cleanedUtr.isBlank() && !cleanedUtr.matches("^\\d{12}$")) {
            throw new BadRequestException("UTR must contain exactly 12 digits.");
        }

        // Check if there is already a submitted payment awaiting review
        if (paymentRepository.findByOrderIdAndStatus(order.getId(), "submitted").isPresent()) {
            throw new ConflictException("Payment proof is already submitted and awaiting vendor verification.");
        }

        String proofPath = null;
        if (hasFile) {
            proofPath = storageService.store(proofFile, "payments/" + order.getVendorId() + "/" + order.getId());
        }

        PaymentSubmission submission = PaymentSubmission.builder()
                .orderId(order.getId())
                .vendorId(order.getVendorId())
                .customerAuthId(customerId)
                .amount(order.getTotal())
                .utrNumber(cleanedUtr)
                .proofPath(proofPath)
                .status("submitted")
                .submittedAt(Instant.now())
                .build();
        submission.setTenantId(order.getTenantId());

        submission = paymentRepository.save(submission);

        order.setPaymentReviewStatus("submitted");
        orderRepository.save(order);

        return submission;
    }

    @Transactional
    public PaymentSubmission reviewPayment(UUID paymentId, String status, String note) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        PaymentSubmission submission = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment submission", "id", paymentId));

        if (user.getVendorId() != null && !user.getVendorId().equals(submission.getVendorId())) {
            throw new ForbiddenException("You can only review payments for your store.");
        }

        List<String> allowed = List.of("verified", "rejected");
        if (!allowed.contains(status.toLowerCase())) {
            throw new BadRequestException("Invalid payment review status. Choose verified or rejected.");
        }

        Order order = orderRepository.findById(submission.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", submission.getOrderId()));

        submission.setStatus(status.toLowerCase());
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(user.getId());
        submission.setReviewNote(note);

        if ("verified".equalsIgnoreCase(status)) {
            order.setPaymentStatus("paid");
            order.setPaymentReviewStatus("verified");
        } else {
            order.setPaymentReviewStatus("rejected");
        }
        orderRepository.save(order);

        auditService.log(submission.getTenantId(), user.getId(), "REVIEW_PAYMENT",
                "marketplace_payment_submissions", paymentId.toString(), "Payment status set to " + status);

        return paymentRepository.save(submission);
    }
}
