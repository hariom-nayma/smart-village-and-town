package com.smartvillage.modules.delivery.controller;

import com.smartvillage.common.dto.ApiResponse;
import com.smartvillage.modules.delivery.entity.DeliveryAgent;
import com.smartvillage.modules.delivery.entity.DeliveryAssignment;
import com.smartvillage.modules.delivery.repository.DeliveryAgentRepository;
import com.smartvillage.modules.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin Delivery Management", description = "Assign delivery agents, manage delivery fleet")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryAgentRepository deliveryAgentRepository;

    @PostMapping("/assign-delivery")
    @Operation(summary = "Assign a ready order to a delivery partner")
    public ApiResponse<DeliveryAssignment> assignDelivery(@RequestBody Map<String, UUID> body) {
        UUID orderId = body.get("order_id");
        UUID agentId = body.get("delivery_agent_id");
        return ApiResponse.ok("Delivery partner assigned successfully", deliveryService.assignDelivery(orderId, agentId));
    }

    @GetMapping("/delivery-agents")
    @Operation(summary = "List active delivery agents and profile requests for a tenant")
    public ApiResponse<Map<String, Object>> listAgents(@RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok(deliveryService.getAdminAgents(tenant));
    }

    @PostMapping("/delivery-agents")
    @Operation(summary = "Register a new delivery partner and generate initial credentials")
    public ApiResponse<Map<String, Object>> createAgent(@RequestParam("full_name") String fullName,
                                                        @RequestParam("email") String email,
                                                        @RequestParam("phone") String phone,
                                                        @RequestParam(value = "address", required = false) String address,
                                                        @RequestParam(value = "vehicle_type", required = false) String vehicleType,
                                                        @RequestParam(value = "vehicle_number", required = false) String vehicleNumber,
                                                        @RequestParam(value = "tenant", required = false) String tenant) {
        return ApiResponse.ok("Delivery partner account created",
                deliveryService.createDeliveryAgent(fullName, email, phone, address, vehicleType, vehicleNumber, tenant));
    }
}
