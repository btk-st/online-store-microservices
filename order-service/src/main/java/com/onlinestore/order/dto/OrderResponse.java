package com.onlinestore.order.dto;

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
public class OrderResponse {

	private UUID id;
	private UUID userId;
	private String username;
	private List<OrderItemResponse> items;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderItemResponse {
		private UUID id;
		private UUID productId;
		private String productName;
		private Integer quantity;
		private BigDecimal price;
		private BigDecimal sale;
		private BigDecimal totalPrice;
	}

}
