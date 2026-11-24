package com.smartvillage.common.security;

import com.smartvillage.common.constant.RoleConstants;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("User is not authenticated");
    }

    public static java.util.Optional<UserPrincipal> getCurrentUserOpt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return java.util.Optional.of(principal);
        }
        return java.util.Optional.empty();
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static boolean isSuperAdmin() {
        try {
            return RoleConstants.SUPER_ADMIN.equalsIgnoreCase(getCurrentUser().getRole());
        } catch (Exception e) {
            return false;
        }
    }

    public static void validateTenantAccess(UUID tenantId) {
        UserPrincipal user = getCurrentUser();
        if (RoleConstants.SUPER_ADMIN.equalsIgnoreCase(user.getRole())) {
            return;
        }
        if (tenantId == null || !tenantId.equals(user.getTenantId())) {
            throw new ForbiddenException("You do not have access to this tenant's data.");
        }
    }
}
