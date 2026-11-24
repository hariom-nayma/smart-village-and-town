package com.smartvillage.modules.civic.repository;

import com.smartvillage.modules.civic.entity.ComplaintUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintUpdateRepository extends JpaRepository<ComplaintUpdate, UUID> {
    List<ComplaintUpdate> findByComplaintIdOrderByCreatedAtAsc(UUID complaintId);
}
