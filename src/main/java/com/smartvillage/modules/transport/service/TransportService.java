package com.smartvillage.modules.transport.service;

import com.smartvillage.common.audit.AuditService;
import com.smartvillage.common.context.TenantContext;
import com.smartvillage.common.exception.BadRequestException;
import com.smartvillage.common.exception.ConflictException;
import com.smartvillage.common.exception.ForbiddenException;
import com.smartvillage.common.exception.ResourceNotFoundException;
import com.smartvillage.common.security.SecurityUtils;
import com.smartvillage.common.security.UserPrincipal;
import com.smartvillage.common.storage.StorageService;
import com.smartvillage.common.util.CodeGenerator;
import com.smartvillage.modules.delivery.entity.DeliveryAgent;
import com.smartvillage.modules.delivery.repository.DeliveryAgentRepository;
import com.smartvillage.modules.marketplace.entity.CustomerProfile;
import com.smartvillage.modules.marketplace.repository.CustomerProfileRepository;
import com.smartvillage.modules.tenant.entity.Tenant;
import com.smartvillage.modules.tenant.service.TenantService;
import com.smartvillage.modules.transport.dto.CreateTransportBookingRequest;
import com.smartvillage.modules.transport.entity.TransportBooking;
import com.smartvillage.modules.transport.entity.TransportRate;
import com.smartvillage.modules.transport.entity.TransportVehicle;
import com.smartvillage.modules.transport.repository.TransportBookingRepository;
import com.smartvillage.modules.transport.repository.TransportRateRepository;
import com.smartvillage.modules.transport.repository.TransportVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportService {

    private final TransportVehicleRepository vehicleRepository;
    private final TransportRateRepository rateRepository;
    private final TransportBookingRepository bookingRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TenantService tenantService;
    private final StorageService storageService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Map<String, Object> getCatalog(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : "gursarai";
        Tenant tenant = tenantService.getTenantEntityBySlug(slug);

        List<TransportVehicle> vehicles = vehicleRepository.findByTenantIdAndApprovalStatusAndIsActiveTrue(tenant.getId(), "approved");
        List<DeliveryAgent> agents = deliveryAgentRepository.findByTenantIdAndIsActiveTrue(tenant.getId());
        Map<UUID, DeliveryAgent> agentMap = agents.stream().collect(Collectors.toMap(DeliveryAgent::getId, a -> a, (k1, k2) -> k1, HashMap::new));

        List<Map<String, Object>> vehicleCards = new ArrayList<>();
        for (TransportVehicle v : vehicles) {
            List<TransportRate> rates = rateRepository.findByVehicleId(v.getId());
            DeliveryAgent driver = agentMap.get(v.getDeliveryAgentId());

            Map<String, Object> card = new HashMap<>();
            card.put("vehicle", v);
            card.put("driverName", driver != null ? driver.getFullName() : "Driver");
            card.put("driverAvailability", driver != null ? driver.getAvailabilityStatus() : "offline");
            card.put("rates", rates);
            vehicleCards.add(card);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("vehicles", vehicleCards);
        res.put("tenant", tenant);
        return res;
    }

    @Transactional(readOnly = true)
    public List<TransportVehicle> getMyVehicles() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery partner account is required."));
        return vehicleRepository.findByDeliveryAgentId(agent.getId());
    }

    @Transactional
    public TransportVehicle registerVehicle(String vehicleType, String makeModel, String registrationNumber,
                                           int passengerCapacity, BigDecimal cargoWeightKg, BigDecimal cargoAreaSqft,
                                           boolean supportsPassengers, boolean supportsGoods,
                                           MultipartFile vehiclePhoto, MultipartFile rcDoc, MultipartFile licenseDoc) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery partner account is required."));

        String cleanReg = registrationNumber.toUpperCase().replaceAll("\\s+", "");
        if (vehicleRepository.findByTenantIdAndRegistrationNumber(agent.getTenantId(), cleanReg).isPresent()) {
            throw new ConflictException("Vehicle with this registration number already registered.");
        }

        String photoPath = vehiclePhoto != null && !vehiclePhoto.isEmpty() ? storageService.store(vehiclePhoto, "transport/" + agent.getId()) : null;
        String rcPath = rcDoc != null && !rcDoc.isEmpty() ? storageService.store(rcDoc, "transport/" + agent.getId()) : null;
        String licPath = licenseDoc != null && !licenseDoc.isEmpty() ? storageService.store(licenseDoc, "transport/" + agent.getId()) : null;

        TransportVehicle vehicle = TransportVehicle.builder()
                .deliveryAgentId(agent.getId())
                .vehicleType(vehicleType.toLowerCase())
                .makeModel(makeModel.trim())
                .registrationNumber(cleanReg)
                .passengerCapacity(passengerCapacity)
                .cargoWeightKg(cargoWeightKg != null ? cargoWeightKg : BigDecimal.ZERO)
                .cargoAreaSqft(cargoAreaSqft != null ? cargoAreaSqft : BigDecimal.ZERO)
                .supportsPassengers(supportsPassengers)
                .supportsGoods(supportsGoods)
                .vehiclePhotoPath(photoPath)
                .rcDocumentPath(rcPath)
                .drivingLicencePath(licPath)
                .approvalStatus("pending")
                .isActive(true)
                .build();
        vehicle.setTenantId(agent.getTenantId());

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public TransportRate saveRate(UUID vehicleId, String serviceType, BigDecimal ratePerKm, BigDecimal minimumFare, BigDecimal waitingRate) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery partner account is required."));

        TransportVehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("TransportVehicle", "id", vehicleId));

        if (!vehicle.getDeliveryAgentId().equals(agent.getId())) {
            throw new ForbiddenException("This vehicle does not belong to your account.");
        }

        TransportRate rate = rateRepository.findByVehicleIdAndServiceType(vehicle.getId(), serviceType.toLowerCase())
                .orElseGet(() -> {
                    TransportRate r = TransportRate.builder()
                            .deliveryAgentId(agent.getId())
                            .vehicleId(vehicle.getId())
                            .serviceType(serviceType.toLowerCase())
                            .build();
                    r.setTenantId(agent.getTenantId());
                    return r;
                });

        rate.setRatePerKm(ratePerKm);
        rate.setMinimumFare(minimumFare != null ? minimumFare : BigDecimal.ZERO);
        rate.setWaitingRatePerHour(waitingRate != null ? waitingRate : BigDecimal.ZERO);
        rate.setActive(true);

        return rateRepository.save(rate);
    }

    @Transactional
    public TransportBooking createBooking(CreateTransportBookingRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        String slug = request.getTenant() != null ? request.getTenant() : "gursarai";
        Tenant tenant = tenantService.getTenantEntityBySlug(slug);

        CustomerProfile customer = customerProfileRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("An approved customer account is required to book transport."));

        if (!customer.isActive() || !"approved".equalsIgnoreCase(customer.getApprovalStatus())) {
            throw new ForbiddenException("Customer account must be approved before booking.");
        }

        // Find candidate vehicles & rates
        List<TransportVehicle> candidateVehicles = vehicleRepository.findByTenantIdAndApprovalStatusAndIsActiveTrue(tenant.getId(), "approved")
                .stream()
                .filter(v -> v.getVehicleType().equalsIgnoreCase(request.getVehicleType()))
                .toList();

        if (candidateVehicles.isEmpty()) {
            throw new ConflictException("No approved vehicle available for type: " + request.getVehicleType());
        }

        TransportVehicle chosenVehicle = null;
        TransportRate chosenRate = null;

        if (request.getVehicleId() != null) {
            chosenVehicle = candidateVehicles.stream().filter(v -> v.getId().equals(request.getVehicleId())).findFirst()
                    .orElseThrow(() -> new BadRequestException("Selected vehicle is not available."));
            chosenRate = rateRepository.findByVehicleIdAndServiceType(chosenVehicle.getId(), request.getServiceType().toLowerCase())
                    .orElseThrow(() -> new BadRequestException("No active rate found for selected vehicle and service type."));
        } else {
            for (TransportVehicle v : candidateVehicles) {
                Optional<TransportRate> r = rateRepository.findByVehicleIdAndServiceType(v.getId(), request.getServiceType().toLowerCase());
                if (r.isPresent()) {
                    chosenVehicle = v;
                    chosenRate = r.get();
                    break;
                }
            }
        }

        if (chosenVehicle == null || chosenRate == null) {
            throw new ConflictException("No vehicle with approved rate found for your requirements.");
        }

        BigDecimal distance = request.getDistanceKm();
        BigDecimal distanceFare = chosenRate.getRatePerKm().multiply(distance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedFare = distanceFare.max(chosenRate.getMinimumFare());

        String bookingNumber = CodeGenerator.generateTransportBookingNumber();

        TransportBooking booking = TransportBooking.builder()
                .bookingNumber(bookingNumber)
                .customerAuthUserId(user.getId())
                .customerName(customer.getFullName())
                .customerPhone(customer.getPhone())
                .serviceType(request.getServiceType().toLowerCase())
                .vehicleType(request.getVehicleType().toLowerCase())
                .vehicleId(chosenVehicle.getId())
                .deliveryAgentId(chosenVehicle.getDeliveryAgentId())
                .rateId(chosenRate.getId())
                .sourceAddress(request.getSourceAddress())
                .sourceLatitude(request.getSourceLatitude())
                .sourceLongitude(request.getSourceLongitude())
                .destinationAddress(request.getDestinationAddress())
                .destinationLatitude(request.getDestinationLatitude())
                .destinationLongitude(request.getDestinationLongitude())
                .distanceKm(distance)
                .travelAt(request.getTravelAt())
                .passengerCount(request.getPassengerCount())
                .goodsDescription(request.getGoodsDescription())
                .packageCount(request.getPackageCount())
                .cargoWeightKg(request.getCargoWeightKg())
                .cargoAreaSqft(request.getCargoAreaSqft())
                .ratePerKm(chosenRate.getRatePerKm())
                .minimumFare(chosenRate.getMinimumFare())
                .estimatedFare(estimatedFare)
                .paymentMethod("pay_driver")
                .paymentStatus("pending")
                .bookingStatus("requested")
                .customerNote(request.getCustomerNote())
                .build();
        booking.setTenantId(tenant.getId());

        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<TransportBooking> getCustomerBookings() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return bookingRepository.findByCustomerAuthUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<TransportBooking> getDriverBookings() {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery partner account required."));
        return bookingRepository.findByDeliveryAgentIdOrderByCreatedAtDesc(agent.getId());
    }

    @Transactional
    public TransportBooking driverBookingAction(UUID bookingId, String status) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        DeliveryAgent agent = deliveryAgentRepository.findByAuthUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Delivery partner account required."));

        TransportBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("TransportBooking", "id", bookingId));

        if (!agent.getId().equals(booking.getDeliveryAgentId())) {
            throw new ForbiddenException("Transport booking not assigned to your account.");
        }

        String targetStatus = status.toLowerCase();
        Instant now = Instant.now();

        if ("accepted".equals(targetStatus)) {
            booking.setAcceptedAt(now);
        } else if ("in_progress".equals(targetStatus)) {
            booking.setStartedAt(now);
        } else if ("completed".equals(targetStatus)) {
            booking.setCompletedAt(now);
            booking.setPaymentStatus("paid");
            booking.setFinalFare(booking.getEstimatedFare());
        } else if ("cancelled".equals(targetStatus) || "rejected".equals(targetStatus)) {
            booking.setCancelledAt(now);
        }

        booking.setBookingStatus(targetStatus);
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminTransportList(String tenantSlug) {
        String slug = tenantSlug != null ? tenantSlug : TenantContext.getTenantSlug();
        Tenant tenant = tenantService.getTenantEntityBySlug(slug != null ? slug : "gursarai");
        List<TransportVehicle> vehicles = vehicleRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());
        List<TransportBooking> bookings = bookingRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());
        Map<String, Object> res = new HashMap<>();
        res.put("vehicles", vehicles);
        res.put("bookings", bookings);
        return res;
    }

    @Transactional
    public TransportVehicle adminReviewVehicle(UUID vehicleId, String status, String note) {
        TransportVehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("TransportVehicle", "id", vehicleId));

        vehicle.setApprovalStatus(status.toLowerCase());
        vehicle.setAdminNote(note);
        vehicle.setReviewedBy(SecurityUtils.getCurrentUserId());
        vehicle.setReviewedAt(Instant.now());

        return vehicleRepository.save(vehicle);
    }
}
