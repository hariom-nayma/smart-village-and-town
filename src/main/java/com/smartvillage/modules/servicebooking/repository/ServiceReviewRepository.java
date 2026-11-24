package com.smartvillage.modules.servicebooking.repository;

import com.smartvillage.modules.servicebooking.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, UUID> {
    Optional<ServiceReview> findByBookingId(UUID bookingId);
    List<ServiceReview> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
}
