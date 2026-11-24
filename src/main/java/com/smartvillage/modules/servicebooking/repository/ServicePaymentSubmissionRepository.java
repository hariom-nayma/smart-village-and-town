package com.smartvillage.modules.servicebooking.repository;

import com.smartvillage.modules.servicebooking.entity.ServicePaymentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicePaymentSubmissionRepository extends JpaRepository<ServicePaymentSubmission, UUID> {
    List<ServicePaymentSubmission> findByBookingIdOrderBySubmittedAtDesc(UUID bookingId);
    Optional<ServicePaymentSubmission> findByBookingIdAndStatus(UUID bookingId, String status);
}
