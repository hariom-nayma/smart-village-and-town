package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.PublicHearing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PublicHearingRepository extends JpaRepository<PublicHearing, UUID> {
    List<PublicHearing> findByTenantIdOrderByHearingDateDesc(UUID tenantId);
    List<PublicHearing> findByTenantIdAndHearingDateGreaterThanEqualOrderByHearingDateAsc(UUID tenantId, LocalDate date);
    long countByTenantId(UUID tenantId);
}
