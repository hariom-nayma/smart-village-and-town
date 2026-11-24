package com.smartvillage.modules.servicebooking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartvillage.common.audit.AuditService;
import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.storage.StorageService;
import com.smartvillage.common.util.CodeGenerator;
import com.smartvillage.modules.marketplace.entity.CustomerProfile;
import com.smartvillage.modules.marketplace.entity.Product;
import com.smartvillage.modules.marketplace.entity.Vendor;
import com.smartvillage.modules.marketplace.repository.CustomerProfileRepository;
import com.smartvillage.modules.marketplace.repository.ProductRepository;
import com.smartvillage.modules.marketplace.repository.VendorRepository;
import com.smartvillage.modules.servicebooking.dto.CreateServiceBookingRequest;
import com.smartvillage.modules.servicebooking.entity.ServiceBooking;
import com.smartvillage.modules.servicebooking.entity.ServicePaymentSubmission;
import com.smartvillage.modules.servicebooking.entity.ServiceReview;
import com.smartvillage.modules.servicebooking.repository.ServiceBookingRepository;
import com.smartvillage.modules.servicebooking.repository.ServicePaymentSubmissionRepository;
import com.smartvillage.modules.servicebooking.repository.ServiceReviewRepository;
import com.smartvillage.modules.tenant.entity.Tenant;
import com.smartvillage.modules.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceBookingService {

    private final ServiceBookingRepository bookingRepository;
    private final ServicePaymentSubmissionRepository servicePaymentRepository;
    private final ServiceReviewRepository serviceReviewRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TenantService tenantService;
    private final StorageService storageService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> createBooking(CreateServiceBookingRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        String slug = request.getTenant() != null ? request.getTenant() : "gursarai";
        Tenant tenant = tenantService.getTenantEntityBySlug(slug);

        CustomerProfile customerProfile = customerProfileRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("An approved customer account is required to book a service."));

        if (!customerProfile.isActive() || !"approved".equalsIgnoreCase(customerProfile.getApprovalStatus())) {
            throw new ForbiddenException("An approved customer account is required to book a service.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Service listing not found: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("This service listing is currently unavailable.");
        }

        Vendor vendor = vendorRepository.findById(product.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", product.getVendorId()));

        if (!vendor.isActive() || !"approved".equalsIgnoreCase(vendor.getStatus())) {
            throw new BadRequestException("Service provider is currently unavailable.");
        }

        BigDecimal bookingCharge = product.getBookingCharge() != null ? product.getBookingCharge() : BigDecimal.ZERO;
        BigDecimal visitingCharge = request.isAtCustomerAddress() && product.getVisitingCharge() != null ? product.getVisitingCharge() : BigDecimal.ZERO;
        BigDecimal upfrontAmount = bookingCharge.add(visitingCharge).setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimate = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal remainingAmount = estimate.subtract(upfrontAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        if (upfrontAmount.compareTo(BigDecimal.ZERO) > 0 && (vendor.getUpiId() == null || vendor.getUpiId().isBlank()) && (vendor.getUpiQrUrl() == null || vendor.getUpiQrUrl().isBlank())) {
            throw new ConflictException("This provider has not configured UPI payment details yet. Contact the provider or choose another service.");
        }

        String bookingNumber = CodeGenerator.generateServiceBookingNumber();

        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("at_customer_address", request.isAtCustomerAddress());
        if (request.isAtCustomerAddress()) {
            addressMap.put("line1", request.getAddressLine1() != null ? request.getAddressLine1() : "");
            addressMap.put("area", request.getArea() != null ? request.getArea() : "");
            addressMap.put("city", request.getCity() != null ? request.getCity() : tenant.getName());
        } else {
            addressMap.put("location_type", "provider_premises");
        }

        String addressJson = "{}";
        try {
            addressJson = objectMapper.writeValueAsString(addressMap);
        } catch (Exception ignored) {}

        ServiceBooking booking = ServiceBooking.builder()
                .vendorId(vendor.getId())
                .productId(product.getId())
                .customerAuthId(user.getId())
                .bookingNumber(bookingNumber)
                .customerName(customerProfile.getFullName())
                .customerPhone(customerProfile.getPhone())
                .requestedFor(request.getRequestedFor())
                .serviceAddress(addressJson)
                .notes(request.getNotes())
                .estimatedTotal(estimate)
                .bookingCharge(bookingCharge)
                .visitingCharge(visitingCharge)
                .upfrontAmount(upfrontAmount)
                .remainingAmount(remainingAmount)
                .remainingPaymentStatus(remainingAmount.compareTo(BigDecimal.ZERO) > 0 ? "pending" : "not_required")
                .paymentStatus(upfrontAmount.compareTo(BigDecimal.ZERO) > 0 ? "pending" : "not_required")
                .bookingStatus(upfrontAmount.compareTo(BigDecimal.ZERO) > 0 ? "pending_payment" : "pending_vendor")
                .build();
        booking.setTenantId(tenant.getId());

        booking = bookingRepository.save(booking);

        String upiIntentUrl = null;
        if (upfrontAmount.compareTo(BigDecimal.ZERO) > 0 && vendor.getUpiId() != null) {
            String pa = URLEncoder.encode(vendor.getUpiId(), StandardCharsets.UTF_8);
            String pn = URLEncoder.encode(vendor.getBusinessName(), StandardCharsets.UTF_8);
            String am = URLEncoder.encode(upfrontAmount.toString(), StandardCharsets.UTF_8);
            String tr = URLEncoder.encode(bookingNumber, StandardCharsets.UTF_8);
            String tn = URLEncoder.encode("Service booking " + bookingNumber, StandardCharsets.UTF_8);
            upiIntentUrl = String.format("upi://pay?pa=%s&pn=%s&am=%s&tr=%s&tn=%s&cu=INR", pa, pn, am, tr, tn);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("booking", booking);
        res.put("productName", product.getName());
        res.put("vendor", vendor);
        res.put("upiIntentUrl", upiIntentUrl);
        return res;
    }

    @Transactional
    public ServicePaymentSubmission submitUpfrontPayment(UUID bookingId, String utr, MultipartFile proofFile) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceBooking", "id", bookingId));

        if (!booking.getCustomerAuthId().equals(user.getId())) {
            throw new ForbiddenException("You can only pay for your own service bookings.");
        }

        if (booking.getUpfrontAmount().compareTo(BigDecimal.ZERO) <= 0 || "paid".equalsIgnoreCase(booking.getPaymentStatus()) || !"pending_payment".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ConflictException("No upfront payment is currently due for this booking.");
        }

        String cleanedUtr = utr != null ? utr.replaceAll("\\s", "") : null;
        boolean hasFile = proofFile != null && !proofFile.isEmpty();

        if ((cleanedUtr == null || cleanedUtr.isBlank()) && !hasFile) {
            throw new BadRequestException("Enter the 12-digit UTR or upload a payment screenshot.");
        }

        if (cleanedUtr != null && !cleanedUtr.isBlank() && !cleanedUtr.matches("^\\d{12}$")) {
            throw new BadRequestException("UTR must contain exactly 12 digits.");
        }

        if (servicePaymentRepository.findByBookingIdAndStatus(booking.getId(), "submitted").isPresent()) {
            throw new ConflictException("Payment proof is already submitted and awaiting provider review.");
        }

        String proofPath = null;
        if (hasFile) {
            proofPath = storageService.store(proofFile, "service-payments/" + booking.getVendorId() + "/" + booking.getId());
        }

        ServicePaymentSubmission payment = ServicePaymentSubmission.builder()
                .bookingId(booking.getId())
                .vendorId(booking.getVendorId())
                .customerAuthId(user.getId())
                .amount(booking.getUpfrontAmount())
                .utrNumber(cleanedUtr)
                .proofPath(proofPath)
                .status("submitted")
                .submittedAt(Instant.now())
                .build();
        payment.setTenantId(booking.getTenantId());

        return servicePaymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<ServiceBooking> getCustomerBookings() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return bookingRepository.findByCustomerAuthIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ServiceBooking> getVendorBookings() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getVendorId() == null) {
            throw new ForbiddenException("Active vendor account is required.");
        }
        return bookingRepository.findByVendorIdOrderByCreatedAtDesc(user.getVendorId());
    }

    @Transactional
    public void updateVendorBookingStatus(UUID bookingId, String newStatus) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceBooking", "id", bookingId));

        if (!booking.getVendorId().equals(user.getVendorId())) {
            throw new ForbiddenException("This service booking does not belong to your store.");
        }

        String targetStatus = newStatus.toLowerCase();
        List<String> allowed = List.of("accepted", "in_progress", "awaiting_customer_confirmation", "cancelled", "rejected");
        if (!allowed.contains(targetStatus)) {
            throw new BadRequestException("Invalid service booking status.");
        }

        if ("awaiting_customer_confirmation".equals(targetStatus)) {
            String code = CodeGenerator.generateCompletionCode();
            booking.setCompletionCode(code);
            booking.setCompletionCodeCreatedAt(Instant.now());
        }

        booking.setBookingStatus(targetStatus);
        bookingRepository.save(booking);
    }

    @Transactional
    public void completeBooking(UUID bookingId, String completionCode) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceBooking", "id", bookingId));

        if (!booking.getVendorId().equals(user.getVendorId())) {
            throw new ForbiddenException("This booking does not belong to your store.");
        }

        if (!"awaiting_customer_confirmation".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ConflictException("The booking must be in awaiting customer confirmation status.");
        }

        if (booking.getCompletionCode() == null || !booking.getCompletionCode().equals(completionCode.trim())) {
            throw new BadRequestException("Customer completion code is incorrect.");
        }

        booking.setBookingStatus("completed");
        booking.setCompletedAt(Instant.now());
        booking.setCompletionCode(null);
        bookingRepository.save(booking);
    }

    @Transactional
    public void cancelBookingByCustomer(UUID bookingId, String reason) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceBooking", "id", bookingId));

        if (!booking.getCustomerAuthId().equals(user.getId())) {
            throw new ForbiddenException("You can only cancel your own bookings.");
        }

        if (List.of("completed", "cancelled", "rejected").contains(booking.getBookingStatus().toLowerCase())) {
            throw new ConflictException("This booking is already closed or completed.");
        }

        // 12-hour cancellation rule
        if (booking.getRequestedFor() != null) {
            Duration duration = Duration.between(Instant.now(), booking.getRequestedFor());
            if (duration.toHours() < 12) {
                throw new ConflictException("Cancellation is only available when at least 12 hours remain before scheduled time. Contact service provider.");
            }
        }

        boolean paid = "paid".equalsIgnoreCase(booking.getPaymentStatus()) && booking.getUpfrontAmount().compareTo(BigDecimal.ZERO) > 0;

        booking.setBookingStatus("cancelled");
        booking.setCancelledBy("customer");
        booking.setCancelledAt(Instant.now());
        booking.setCancellationReason(reason);
        booking.setRefundStatus(paid ? "requested" : "not_applicable");
        booking.setRefundAmount(paid ? booking.getUpfrontAmount() : BigDecimal.ZERO);
        booking.setRefundRequestedAt(paid ? Instant.now() : null);

        bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVendorAnalytics() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        if (user.getVendorId() == null) {
            throw new ForbiddenException("Active vendor account is required.");
        }

        List<ServiceBooking> bookings = bookingRepository.findByVendorId(user.getVendorId());
        long totalCompleted = bookings.stream().filter(b -> "completed".equalsIgnoreCase(b.getBookingStatus())).count();
        BigDecimal totalEarnings = bookings.stream()
                .filter(b -> "completed".equalsIgnoreCase(b.getBookingStatus()))
                .map(b -> b.getUpfrontAmount().add(b.getRemainingPaidAmount() != null ? b.getRemainingPaidAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalBookings", bookings.size());
        analytics.put("completedBookings", totalCompleted);
        analytics.put("totalEarnings", totalEarnings);
        return analytics;
    }

    @Transactional
    public ServiceReview reviewBooking(UUID bookingId, int rating, String reviewText) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceBooking", "id", bookingId));

        if (!booking.getCustomerAuthId().equals(user.getId())) {
            throw new ForbiddenException("You can only review your own service bookings.");
        }
        if (!"completed".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ConflictException("You can only review a completed service booking.");
        }
        if (serviceReviewRepository.findByBookingId(bookingId).isPresent()) {
            throw new ConflictException("This service booking has already been reviewed.");
        }

        CustomerProfile profile = customerProfileRepository.findByAuthUserId(user.getId()).orElse(null);

        ServiceReview review = ServiceReview.builder()
                .bookingId(booking.getId())
                .vendorId(booking.getVendorId())
                .productId(booking.getProductId())
                .customerAuthId(user.getId())
                .customerName(profile != null ? profile.getFullName() : user.getUsername())
                .rating(rating)
                .review(reviewText != null ? reviewText.trim() : null)
                .build();
        review.setTenantId(booking.getTenantId());

        return serviceReviewRepository.save(review);
    }
}
