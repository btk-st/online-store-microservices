package com.onlinestore.notification.controller;

import com.onlinestore.notification.entity.OrderEntity;
import com.onlinestore.notification.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders API", description = "API для работы с заказами")
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "Получить все заказы")
    @ApiResponse(responseCode = "200", description = "Список всех заказов")
    @GetMapping("/all")
    public List<OrderEntity> getAllOrders() {
        return orderService.getAllOrders();
    }

    @Operation(summary = "Получить заказы по ID заказа")
    @ApiResponse(responseCode = "200", description = "Список заказов")
    @ApiResponse(responseCode = "200", description = "Пустой список если заказы не найдены")
    @GetMapping("/order/{orderId}")
    public List<OrderEntity> getOrdersByOrderId(
            @Parameter(description = "ID заказа") @PathVariable UUID orderId) {
        return orderService.getOrdersByOrderId(orderId);
    }

    @Operation(summary = "Получить заказы по ID пользователя")
    @ApiResponse(responseCode = "200", description = "Список заказов пользователя")
    @GetMapping("/user/{userId}")
    public List<OrderEntity> getOrdersByUserId(
            @Parameter(description = "ID пользователя") @PathVariable UUID userId) {
        return orderService.getOrdersByUserId(userId);
    }
}
