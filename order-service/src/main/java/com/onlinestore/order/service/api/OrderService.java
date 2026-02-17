package com.onlinestore.order.service.api;

import java.util.UUID;

import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.dto.OrderResponse;

/**
 * Сервис для работы с заказами.
 */
public interface OrderService {
	OrderResponse createOrder(UUID userId, CreateOrderRequest request);
}
