package com.onlinestore.order.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.onlinestore.order.dto.CreateOrderRequest.OrderItemRequest;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.OrderItem;
import com.onlinestore.order.grpc.ProductAvailabilityResponse;

@Component
public class OrderItemMapper {
	public OrderItem toOrderItem(Order order, OrderItemRequest itemRequest, ProductAvailabilityResponse availability) {
		return OrderItem.builder().order(order).productId(itemRequest.getProductId())
				.productName(availability.getProductName()).quantity(itemRequest.getQuantity())
				.price(BigDecimal.valueOf(availability.getPrice())).sale(BigDecimal.valueOf(availability.getDiscount()))
				.build();
	}
}
