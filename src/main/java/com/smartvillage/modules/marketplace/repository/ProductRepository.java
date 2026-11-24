package com.smartvillage.modules.marketplace.repository;

import com.smartvillage.modules.marketplace.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);
    List<Product> findByVendorIdAndActiveTrueOrderByNameAsc(UUID vendorId);
    List<Product> findByCategoryIdAndActiveTrue(UUID categoryId);
}
