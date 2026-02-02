package com.onlinestore.order.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.onlinestore.order.grpc.ProductAvailabilityRequest;

@Component
public class ProductMapper {
	public ProductAvailabilityRequest toAvailabilityRequest(UUID productId, int quantity) {
		return ProductAvailabilityRequest.newBuilder().setProductId(productId.toString()).setRequestedQuantity(quantity)
				.build();
	}
}
