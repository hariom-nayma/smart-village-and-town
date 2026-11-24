package com.smartvillage.common.context;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TENANT_SLUG = new ThreadLocal<>();

    private TenantContext() {}

    public static UUID getTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static String getTenantSlug() {
        return CURRENT_TENANT_SLUG.get();
    }

    public static void setTenantSlug(String tenantSlug) {
        CURRENT_TENANT_SLUG.set(tenantSlug);
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
        CURRENT_TENANT_SLUG.remove();
    }
}
