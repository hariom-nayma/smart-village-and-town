package com.smartvillage.modules.tenant.service;

import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.util.SlugUtils;
import com.smartvillage.modules.tenant.dto.TenantDto;
import com.smartvillage.modules.tenant.entity.Tenant;
import com.smartvillage.modules.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<TenantDto> getActiveTenants() {
        return tenantRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "slug", slug));
        return mapToDto(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenantEntityBySlug(String slug) {
        return tenantRepository.findBySlug(slug.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "slug", slug));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantEntityById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", id));
    }

    @Transactional
    public TenantDto createTenant(TenantDto request) {
        String slug = SlugUtils.toSlug(request.getName());
        if (tenantRepository.existsBySlug(slug)) {
            throw new ConflictException("A village or town with slug '" + slug + "' already exists.");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .slug(slug)
                .district(request.getDistrict())
                .state(request.getState())
                .pincode(request.getPincode())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .active(true)
                .build();

        return mapToDto(tenantRepository.save(tenant));
    }

    public TenantDto mapToDto(Tenant tenant) {
        return TenantDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .district(tenant.getDistrict())
                .state(tenant.getState())
                .pincode(tenant.getPincode())
                .contactPhone(tenant.getContactPhone())
                .contactEmail(tenant.getContactEmail())
                .active(tenant.isActive())
                .build();
    }
}
