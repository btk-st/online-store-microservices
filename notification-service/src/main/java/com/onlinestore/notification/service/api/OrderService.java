package com.onlinestore.notification.service.api;

import java.util.List;
import java.util.UUID;

import com.onlinestore.notification.dto.OrderDto;

public interface OrderService {
	List<OrderDto> getAllOrders();
	OrderDto getItemsByOrderId(UUID orderId);
	List<OrderDto> getOrdersByUserId(UUID userId);
}
