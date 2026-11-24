package com.smartvillage.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_TENANT_SLUG = "X-Tenant-Slug";
    public static final String PARAM_TENANT = "tenant";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantIdStr = request.getHeader(HEADER_TENANT_ID);
            if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                try {
                    TenantContext.setTenantId(UUID.fromString(tenantIdStr.trim()));
                } catch (IllegalArgumentException ignored) {}
            }

            String slug = request.getHeader(HEADER_TENANT_SLUG);
            if (slug == null || slug.isBlank()) {
                slug = request.getParameter(PARAM_TENANT);
            }
            if (slug != null && !slug.isBlank()) {
                TenantContext.setTenantSlug(slug.trim().toLowerCase());
            } else if (TenantContext.getTenantSlug() == null) {
                // Default fallback tenant slug if not specified
                TenantContext.setTenantSlug("gursarai");
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
