package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.ComplaintFieldVisitItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintFieldVisitItemRepository extends JpaRepository<ComplaintFieldVisitItem, UUID> {
    List<ComplaintFieldVisitItem> findByFieldVisitId(UUID fieldVisitId);
    List<ComplaintFieldVisitItem> findByComplaintId(UUID complaintId);
    void deleteByFieldVisitId(UUID fieldVisitId);
}
