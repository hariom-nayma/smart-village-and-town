package com.smartvillage.modules.auth.service;

import com.smartvillage.common.constant.RoleConstants;
import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.exception.UnauthorizedException;
import com.smartvillage.common.security.JwtTokenProvider;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.modules.auth.dto.*;
import com.smartvillage.modules.auth.entity.MobileOtpVerification;
import com.smartvillage.modules.auth.entity.User;
import com.smartvillage.modules.auth.repository.MobileOtpVerificationRepository;
import com.smartvillage.modules.auth.repository.UserRepository;
import com.smartvillage.modules.delivery.entity.DeliveryAgent;
import com.smartvillage.modules.delivery.repository.DeliveryAgentRepository;
import com.smartvillage.modules.marketplace.entity.CustomerProfile;
import com.smartvillage.modules.marketplace.entity.Vendor;
import com.smartvillage.modules.marketplace.repository.CustomerProfileRepository;
import com.smartvillage.modules.marketplace.repository.VendorRepository;
import com.smartvillage.modules.tenant.entity.Tenant;
import com.smartvillage.modules.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;
    private final CustomerProfileRepository customerProfileRepository;
    private final VendorRepository vendorRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final MobileOtpVerificationRepository mobileOtpVerificationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmailOrPhone().trim();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadRequestException("Your account is currently deactivated. Please contact administrator.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return AuthResponse.builder()
                .token(token)
                .userId(principal.getId())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .role(principal.getRole())
                .tenantId(principal.getTenantId())
                .vendorId(principal.getVendorId())
                .build();
    }

    @Transactional
    public AuthResponse customerPinLogin(CustomerPinLoginRequest request) {
        String mobile = request.getMobile().trim();
        UUID tenantId = null;
        if (request.getTenant() != null && !request.getTenant().isBlank()) {
            tenantId = tenantService.getTenantEntityBySlug(request.getTenant()).getId();
        }

        User user = (tenantId != null ? userRepository.findByPhoneAndTenantId(mobile, tenantId) : userRepository.findByPhone(mobile))
                .orElseThrow(() -> new UnauthorizedException("Customer account not found for this mobile number."));

        if (!user.isActive()) {
            throw new BadRequestException("Your customer account is currently deactivated.");
        }

        if (!passwordEncoder.matches(request.getPin(), user.getPassword())) {
            throw new UnauthorizedException("Invalid mobile number or 8-digit PIN.");
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String token = tokenProvider.generateTokenForUser(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .build();
    }

    @Transactional
    public SendOtpResponse sendLoginOtp(SendOtpRequest request) {
        String rawMobile = request.getMobile();
        String mobile = rawMobile != null ? rawMobile.replaceAll("\\D", "") : "";
        if (mobile.length() != 10 || !mobile.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Please enter a valid 10-digit Indian mobile number (starting with 6, 7, 8, or 9).");
        }

        UUID tenantId = null;
        if (request.getTenant() != null && !request.getTenant().isBlank()) {
            tenantId = tenantService.getTenantEntityBySlug(request.getTenant().trim()).getId();
        }

        Instant now = Instant.now();

        // 1. Rate limiting: max 6 OTP requests in the last 15 minutes
        long recentRequests = mobileOtpVerificationRepository.countByPhoneAndCreatedAtAfter(mobile, now.minus(15, ChronoUnit.MINUTES));
        if (recentRequests >= 6) {
            throw new BadRequestException("Too many OTP requests for this mobile number. Please wait a few minutes before trying again.");
        }

        // 2. Cooldown check: 30 seconds since last OTP
        Optional<MobileOtpVerification> lastOtpOpt = mobileOtpVerificationRepository.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(mobile);
        if (lastOtpOpt.isPresent()) {
            MobileOtpVerification last = lastOtpOpt.get();
            long secondsSinceLast = Duration.between(last.getCreatedAt(), now).getSeconds();
            if (secondsSinceLast < 30) {
                long remaining = 30 - secondsSinceLast;
                throw new BadRequestException("Please wait " + remaining + " seconds before requesting a new OTP.");
            }
        }

        // 3. Invalidate any existing pending unverified OTPs
        List<MobileOtpVerification> pending = mobileOtpVerificationRepository.findByPhoneAndVerifiedFalse(mobile);
        for (MobileOtpVerification p : pending) {
            p.setExpiresAt(now);
        }
        if (!pending.isEmpty()) {
            mobileOtpVerificationRepository.saveAll(pending);
        }

        // 4. Generate 6-digit cryptographically secure OTP
        int code = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(code);

        String purpose = request.getPurpose() != null && !request.getPurpose().isBlank() ? request.getPurpose().trim().toUpperCase() : "LOGIN";

        MobileOtpVerification record = MobileOtpVerification.builder()
                .tenantId(tenantId)
                .phone(mobile)
                .otpCode(otpCode)
                .purpose(purpose)
                .attempts(0)
                .maxAttempts(5)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .verified(false)
                .build();
        mobileOtpVerificationRepository.save(record);

        // 5. Dispatch SMS notification / simulation
        String maskedPhone = "******" + mobile.substring(6);
        log.info("📱 [SMS-GATEWAY] Outbound OTP for +91 {}: '{}' (Purpose: {}, Valid for 5 minutes)", mobile, otpCode, purpose);

        return SendOtpResponse.builder()
                .mobile(mobile)
                .message("OTP sent successfully to +91 " + maskedPhone + ". Valid for 5 minutes.")
                .cooldownSeconds(30)
                .expiryMinutes(5)
                .debugOtp(otpCode)
                .build();
    }

    @Transactional
    public AuthResponse verifyOtpLogin(VerifyOtpLoginRequest request) {
        String rawMobile = request.getMobile();
        String mobile = rawMobile != null ? rawMobile.replaceAll("\\D", "") : "";
        String enteredOtp = request.getOtp() != null ? request.getOtp().trim() : "";

        if (mobile.length() != 10) {
            throw new BadRequestException("Invalid mobile number.");
        }
        if (enteredOtp.length() != 6) {
            throw new BadRequestException("OTP must be exactly 6 digits.");
        }

        Instant now = Instant.now();
        MobileOtpVerification otpRecord = mobileOtpVerificationRepository
                .findTopByPhoneAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(mobile, "LOGIN")
                .or(() -> mobileOtpVerificationRepository.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(mobile))
                .orElseThrow(() -> new BadRequestException("No active OTP found for this mobile number. Please request an OTP first."));

        if (otpRecord.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        if (otpRecord.getAttempts() >= otpRecord.getMaxAttempts()) {
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        otpRecord.setAttempts(otpRecord.getAttempts() + 1);

        // Verify match (also allow master dev OTP "123456" for automated testing/sandbox)
        boolean isMatch = otpRecord.getOtpCode().equals(enteredOtp) || "123456".equals(enteredOtp);
        if (!isMatch) {
            mobileOtpVerificationRepository.save(otpRecord);
            int remaining = otpRecord.getMaxAttempts() - otpRecord.getAttempts();
            throw new UnauthorizedException("Incorrect OTP code. " + remaining + " attempts remaining.");
        }

        // Mark OTP as verified
        otpRecord.setVerified(true);
        otpRecord.setVerifiedAt(now);
        mobileOtpVerificationRepository.save(otpRecord);

        // Determine tenant
        UUID tenantId = otpRecord.getTenantId();
        if (tenantId == null && request.getTenant() != null && !request.getTenant().isBlank()) {
            tenantId = tenantService.getTenantEntityBySlug(request.getTenant().trim()).getId();
        }
        if (tenantId == null) {
            tenantId = tenantService.getTenantEntityBySlug("gursarai").getId();
        }

        // Locate or auto-provision user
        final UUID finalTenantId = tenantId;
        User user = userRepository.findByPhone(mobile).orElseGet(() -> {
            log.info("Auto-registering new customer account for mobile number: {}", mobile);
            String name = (request.getFullName() != null && !request.getFullName().isBlank())
                    ? request.getFullName().trim()
                    : "Citizen " + mobile.substring(6);
            String customerEmail = "customer-" + mobile + "@marketplace.invalid";

            User newUser = User.builder()
                    .email(customerEmail)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(name)
                    .phone(mobile)
                    .role(RoleConstants.CUSTOMER)
                    .tenantId(finalTenantId)
                    .active(true)
                    .build();
            newUser = userRepository.save(newUser);

            CustomerProfile profile = CustomerProfile.builder()
                    .authUserId(newUser.getId())
                    .tenantId(finalTenantId)
                    .fullName(name)
                    .email(customerEmail)
                    .phone(mobile)
                    .approvalStatus("approved")
                    .active(true)
                    .build();
            customerProfileRepository.save(profile);
            return newUser;
        });

        if (!user.isActive()) {
            throw new BadRequestException("Your account is currently deactivated. Please contact administrator.");
        }

        // Ensure user has an approved CustomerProfile if role is CUSTOMER
        if (RoleConstants.CUSTOMER.equalsIgnoreCase(user.getRole())) {
            customerProfileRepository.findByAuthUserId(user.getId()).ifPresent(p -> {
                if ("pending".equalsIgnoreCase(p.getApprovalStatus())) {
                    p.setApprovalStatus("approved");
                    p.setApprovedAt(Instant.now());
                    customerProfileRepository.save(p);
                }
            });
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String token = tokenProvider.generateTokenForUser(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .vendorId(user.getVendorId())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with email " + request.getEmail() + " already exists.");
        }

        String rawPhone = request.getPhone();
        String phone = (rawPhone != null && !rawPhone.isBlank()) ? rawPhone.replaceAll("\\D", "").trim() : null;
        if (phone != null && !phone.isBlank() && userRepository.findByPhone(phone).isPresent()) {
            throw new ConflictException("An account with mobile number " + phone + " already exists.");
        }

        UUID tenantId = request.getTenantId();
        if (tenantId == null && request.getTenantSlug() != null && !request.getTenantSlug().isBlank()) {
            Tenant tenant = tenantService.getTenantEntityBySlug(request.getTenantSlug());
            tenantId = tenant.getId();
        }

        String requestedRole = request.getRole() != null ? request.getRole().toUpperCase().trim() : RoleConstants.CUSTOMER;

        // Role escalation defense:
        // Any role other than CUSTOMER is considered privileged and requires administrative authorization.
        if (!RoleConstants.CUSTOMER.equalsIgnoreCase(requestedRole)) {
            Optional<UserPrincipal> callerOpt = SecurityUtils.getCurrentUserOpt();
            if (callerOpt.isEmpty()) {
                throw new ForbiddenException("Public self-registration cannot assign administrative or staff roles. Please sign in as an authorized administrator to provision staff accounts.");
            }

            UserPrincipal caller = callerOpt.get();
            boolean isCallerSuperAdmin = RoleConstants.SUPER_ADMIN.equalsIgnoreCase(caller.getRole());
            boolean isCallerAdmin = RoleConstants.ADMIN.equalsIgnoreCase(caller.getRole()) || RoleConstants.MLA.equalsIgnoreCase(caller.getRole());

            if (!isCallerSuperAdmin && !isCallerAdmin) {
                throw new ForbiddenException("You do not have administrative permission to provision privileged accounts.");
            }

            if (!isCallerSuperAdmin) {
                if (RoleConstants.SUPER_ADMIN.equalsIgnoreCase(requestedRole)) {
                    throw new ForbiddenException("Only SUPER_ADMIN can create another SUPER_ADMIN.");
                }
                if (RoleConstants.ADMIN.equalsIgnoreCase(requestedRole) || RoleConstants.MLA.equalsIgnoreCase(requestedRole)) {
                    throw new ForbiddenException("Only SUPER_ADMIN can provision constituency ADMIN or MLA accounts.");
                }
                tenantId = caller.getTenantId();
            }
        }

        if (tenantId == null) {
            Tenant defaultTenant = tenantService.getTenantEntityBySlug("gursarai");
            tenantId = defaultTenant.getId();
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(phone)
                .role(requestedRole)
                .tenantId(tenantId)
                .active(true)
                .build();

        user = userRepository.save(user);

        if (RoleConstants.CUSTOMER.equalsIgnoreCase(requestedRole)) {
            CustomerProfile profile = CustomerProfile.builder()
                    .authUserId(user.getId())
                    .tenantId(tenantId)
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .approvalStatus("approved")
                    .active(true)
                    .build();
            customerProfileRepository.save(profile);
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String token = tokenProvider.generateTokenForUser(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUserDto() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByTenant(UUID tenantId) {
        return userRepository.findByTenantId(tenantId).stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminUsers(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");

        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        boolean superOnly = currentUser != null && RoleConstants.SUPER_ADMIN.equals(currentUser.getRole());

        List<CustomerProfile> customers = customerProfileRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());
        List<Vendor> vendors = vendorRepository.findByTenantIdOrderByBusinessNameAsc(tenant.getId());
        List<DeliveryAgent> agents = deliveryAgentRepository.findByTenantIdOrderByFullNameAsc(tenant.getId());
        List<User> users = userRepository.findByTenantId(tenant.getId());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CustomerProfile c : customers) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getAuthUserId());
            m.put("record_id", c.getAuthUserId());
            m.put("type", "customer");
            m.put("name", c.getFullName());
            m.put("email", c.getEmail() != null ? c.getEmail() : "");
            m.put("phone", c.getPhone() != null ? c.getPhone() : "");
            m.put("status", c.getApprovalStatus() != null ? c.getApprovalStatus() : "registered");
            m.put("active", c.isActive());
            m.put("created_at", c.getCreatedAt());
            m.put("details", Map.of("approval_status", c.getApprovalStatus() != null ? c.getApprovalStatus() : ""));
            rows.add(m);
        }

        for (Vendor v : vendors) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId());
            m.put("record_id", v.getId());
            m.put("auth_user_id", v.getCreatedBy());
            m.put("type", "vendor");
            m.put("name", v.getBusinessName());
            m.put("email", v.getEmail() != null ? v.getEmail() : "");
            m.put("phone", v.getPhone() != null ? v.getPhone() : "");
            m.put("status", v.getStatus() != null ? v.getStatus() : "registered");
            m.put("active", v.isActive());
            m.put("created_at", v.getCreatedAt());
            m.put("details", Map.of("business_name", v.getBusinessName(), "owner_name", v.getOwnerName() != null ? v.getOwnerName() : ""));
            rows.add(m);
        }

        for (DeliveryAgent a : agents) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("record_id", a.getId());
            m.put("auth_user_id", a.getAuthUserId());
            m.put("type", "delivery");
            m.put("name", a.getFullName());
            m.put("email", a.getEmail() != null ? a.getEmail() : "");
            m.put("phone", a.getPhone() != null ? a.getPhone() : "");
            m.put("status", a.getAvailabilityStatus() != null ? a.getAvailabilityStatus() : "offline");
            m.put("active", a.isActive());
            m.put("created_at", a.getCreatedAt());
            m.put("details", Map.of(
                    "address", a.getAddress() != null ? a.getAddress() : "",
                    "vehicle_type", a.getVehicleType() != null ? a.getVehicleType() : "",
                    "vehicle_number", a.getVehicleNumber() != null ? a.getVehicleNumber() : ""
            ));
            rows.add(m);
        }

        for (User u : users) {
            if (!superOnly && RoleConstants.SUPER_ADMIN.equals(u.getRole())) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("record_id", u.getId());
            String t = RoleConstants.ADMIN.equals(u.getRole()) || RoleConstants.SUPER_ADMIN.equals(u.getRole()) ? "admin"
                    : RoleConstants.VENDOR.equals(u.getRole()) ? "vendor" : "profile";
            m.put("type", t);
            m.put("name", u.getFullName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone() != null ? u.getPhone() : "");
            m.put("status", u.getRole());
            m.put("active", u.isActive());
            m.put("created_at", u.getCreatedAt());
            m.put("details", Map.of("role", u.getRole()));
            rows.add(m);
        }

        return Map.of(
                "users", rows,
                "can_manage_admins", superOnly,
                "tenant", tenant.getSlug()
        );
    }

    @Transactional
    public void updateAdminUserStatus(UUID targetId, String type, boolean active, String tenantSlug) {
        if ("customer".equalsIgnoreCase(type)) {
            customerProfileRepository.findByAuthUserId(targetId).ifPresent(p -> {
                p.setActive(active);
                customerProfileRepository.save(p);
            });
            userRepository.findById(targetId).ifPresent(u -> {
                u.setActive(active);
                userRepository.save(u);
            });
        } else if ("vendor".equalsIgnoreCase(type)) {
            vendorRepository.findById(targetId).ifPresent(v -> {
                v.setActive(active);
                vendorRepository.save(v);
                if (v.getCreatedBy() != null) {
                    userRepository.findById(v.getCreatedBy()).ifPresent(u -> {
                        u.setActive(active);
                        userRepository.save(u);
                    });
                }
            });
        } else if ("delivery".equalsIgnoreCase(type)) {
            deliveryAgentRepository.findById(targetId).ifPresent(a -> {
                a.setActive(active);
                deliveryAgentRepository.save(a);
                if (a.getAuthUserId() != null) {
                    userRepository.findById(a.getAuthUserId()).ifPresent(u -> {
                        u.setActive(active);
                        userRepository.save(u);
                    });
                }
            });
        } else {
            userRepository.findById(targetId).ifPresent(u -> {
                u.setActive(active);
                userRepository.save(u);
            });
        }
    }

    @Transactional
    public UserDto updateUserStatus(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setActive(active);
        return mapToDto(userRepository.save(user));
    }

    public UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .vendorId(user.getVendorId())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
