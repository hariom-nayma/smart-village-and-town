package com.smartvillage.modules.auth.entity;

import com.smartvillage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_phone", columnList = "phone"),
        @Index(name = "idx_users_tenant_role", columnList = "tenant_id, role")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "constituency", length = 150)
    private String constituency;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
