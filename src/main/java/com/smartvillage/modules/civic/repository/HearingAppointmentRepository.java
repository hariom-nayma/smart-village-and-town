package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.HearingAppointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HearingAppointmentRepository extends JpaRepository<HearingAppointment, UUID> {
    List<HearingAppointment> findByTenantIdOrderByPreferredDateDesc(UUID tenantId);
    Page<HearingAppointment> findByTenantIdOrderByPreferredDateDesc(UUID tenantId, Pageable pageable);
    List<HearingAppointment> findByHearingIdOrderBySlotNumberAsc(UUID hearingId);
    List<HearingAppointment> findByTenantIdAndCitizenPhoneOrderByCreatedAtDesc(UUID tenantId, String phone);
    long countByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantId(UUID tenantId);
}
