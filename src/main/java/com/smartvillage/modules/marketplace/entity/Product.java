package com.smartvillage.modules.marketplace.entity;

import com.smartvillage.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "marketplace_products", indexes = {
        @Index(name = "idx_mp_products_tenant_active", columnList = "tenant_id, active"),
        @Index(name = "idx_mp_products_vendor", columnList = "vendor_id, active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends TenantEntity {

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "name_hi", length = 200)
    private String nameHi;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "marketplace_product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String unit = "piece";

    @Builder.Default
    @Column(name = "stock_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "min_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal minQuantity = BigDecimal.ONE;

    @Column(name = "max_quantity", precision = 12, scale = 3)
    private BigDecimal maxQuantity;

    @Builder.Default
    @Column(name = "booking_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookingCharge = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "visiting_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal visitingCharge = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String attributes; // JSON string for attributes (service_type, booking_required)

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean featured = false;
}
