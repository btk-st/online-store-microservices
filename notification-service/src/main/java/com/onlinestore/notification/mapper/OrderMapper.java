package com.onlinestore.notification.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.dto.OrderDto.OrderItemDto;
import com.onlinestore.notification.entity.OrderEntity;
import com.onlinestore.notification.kafka.OrderCreatedEvent.OrderItemEvent;

@Component
public class OrderMapper {
	public OrderDto toOrderDto(List<OrderEntity> orderEntities) {
		if (orderEntities == null || orderEntities.isEmpty()) {
			return null;
		}

		// Берем userId, orderId из первой записи (у всех в группе одинаковый)
		UUID userId = orderEntities.get(0).getUserId();
		UUID orderId = orderEntities.get(0).getOrderId();

		// Мапим items
		List<OrderItemDto> items = orderEntities.stream().map(this::toOrderItemDto).collect(Collectors.toList());

		OrderDto order = OrderDto.builder().orderId(orderId).userId(userId).items(items).build();

		// Считаем итоговую сумму
		order.setTotalPrice(order.calculateTotalPrice());

		return order;
	}

	public OrderItemDto toOrderItemDto(OrderEntity entity) {
		return OrderItemDto.builder().productId(entity.getProductId()).quantity(entity.getQuantity())
				.price(entity.getPrice()).sale(entity.getSale()).totalPrice(entity.getTotalPrice()).build();
	}

	public OrderEntity toOrderEntity(OrderItemEvent orderItemEvent, UUID orderId, UUID userId) {
		BigDecimal discountMultiplier = BigDecimal.ONE
				.subtract(orderItemEvent.getSale().divide(BigDecimal.valueOf(100)));

		BigDecimal itemTotalPrice = orderItemEvent.getPrice().multiply(BigDecimal.valueOf(orderItemEvent.getQuantity()))
				.multiply(discountMultiplier);

		return OrderEntity.builder().orderId(orderId).userId(userId).productId(orderItemEvent.getProductId())
				.quantity(orderItemEvent.getQuantity()).price(orderItemEvent.getPrice()).sale(orderItemEvent.getSale())
				.totalPrice(itemTotalPrice).build();
	}
}
