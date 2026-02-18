package com.onlinestore.order.kafka;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

	private UUID orderId;
	private UUID userId;
	private List<OrderItemEvent> items;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderItemEvent {
		private UUID productId;
		private Integer quantity;
		private BigDecimal price;
		private BigDecimal sale;
	}

}
