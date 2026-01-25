package com.onlinestore.order.controller;

import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.dto.OrderResponse;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management API")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create new order")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOrderRequest request) {

        Order order = orderService.createOrder(user.getId(), request);
        OrderResponse response = OrderResponse.fromEntity(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
