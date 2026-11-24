package com.smartvillage.modules.payment.repository;

import com.smartvillage.modules.payment.entity.PaymentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentSubmissionRepository extends JpaRepository<PaymentSubmission, UUID> {
    List<PaymentSubmission> findByVendorIdOrderBySubmittedAtDesc(UUID vendorId);
    List<PaymentSubmission> findByTenantIdOrderBySubmittedAtDesc(UUID tenantId);
    Optional<PaymentSubmission> findByOrderIdAndStatus(UUID orderId, String status);
}
