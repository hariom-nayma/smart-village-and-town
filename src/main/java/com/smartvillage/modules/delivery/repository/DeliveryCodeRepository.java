package com.smartvillage.modules.delivery.repository;

import com.smartvillage.modules.delivery.entity.DeliveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryCodeRepository extends JpaRepository<DeliveryCode, UUID> {
}
