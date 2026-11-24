package com.smartvillage.modules.auth.repository;

import com.smartvillage.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByPhoneAndTenantId(String phone, UUID tenantId);
    boolean existsByEmail(String email);
    List<User> findByTenantIdAndRole(UUID tenantId, String role);
    List<User> findByTenantId(UUID tenantId);
}
