package com.smartvillage.modules.servicebooking.repository;

import com.smartvillage.modules.servicebooking.entity.ServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, UUID> {
    Optional<ServiceBooking> findByBookingNumber(String bookingNumber);
    List<ServiceBooking> findByCustomerAuthIdOrderByCreatedAtDesc(UUID customerAuthId);
    List<ServiceBooking> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
    List<ServiceBooking> findByVendorId(UUID vendorId);
}
