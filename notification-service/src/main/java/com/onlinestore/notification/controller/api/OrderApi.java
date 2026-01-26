package com.onlinestore.notification.controller.api;

import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.dto.OrderDto.OrderItemDto;
import com.onlinestore.notification.entity.OrderEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Tag(name = "Orders API", description = "API for order management")
@RequestMapping("/api/orders")
public interface OrderApi {

    @Operation(
            summary = "Get all orders",
            description = "Retrieves a list of all orders in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved list of all orders"
    )
    @GetMapping("/all")
    List<OrderDto> getAllOrders();

    @Operation(
            summary = "Get items by order ID",
            description = "Retrieves items based on the specified order ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved items for the specified order ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Returns empty list if no items found for the specified order ID"
    )
    @GetMapping("/order/{orderId}")
    OrderDto getItemsByOrderId(
            @Parameter(
                    description = "Unique identifier of the order",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID orderId
    );

    @Operation(
            summary = "Get orders by user ID",
            description = "Retrieves all orders for a specific user"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user's orders"
    )
    @GetMapping("/user/{userId}")
    List<OrderDto> getOrdersByUserId(
            @Parameter(
                    description = "Unique identifier of the user",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID userId
    );
}