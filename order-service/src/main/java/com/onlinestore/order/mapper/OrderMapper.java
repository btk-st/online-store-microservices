package com.onlinestore.order.mapper;

import org.springframework.stereotype.Component;

import com.onlinestore.order.dto.OrderResponse;
import com.onlinestore.order.dto.OrderResponse.OrderItemResponse;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.kafka.OrderCreatedEvent;
import com.onlinestore.order.kafka.OrderCreatedEvent.OrderItemEvent;

@Component
public class OrderMapper {
	public OrderResponse toOrderResponse(Order order) {
		return OrderResponse.builder().id(order.getId()).userId(order.getUser().getId())
				.username(order.getUser().getUsername())
				.items(order.getItems().stream()
						.map(item -> OrderItemResponse.builder().id(item.getId()).productId(item.getProductId())
								.productName(item.getProductName()).quantity(item.getQuantity()).price(item.getPrice())
								.sale(item.getSale()).totalPrice(item.getTotalPrice()).build())
						.toList())
				.build();
	}

	public OrderCreatedEvent toOrderCreatedEvent(Order order) {
		return OrderCreatedEvent.builder().orderId(order.getId()).userId(order.getUser().getId()).items(order.getItems()
				.stream().map(item -> OrderItemEvent.builder().productId(item.getProductId())
						.quantity(item.getQuantity()).price(item.getPrice()).sale(item.getSale()).build())
				.toList()).build();
	}
}
