package com.onlinestore.order.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.onlinestore.order.dto.CreateOrderRequest.OrderItemRequest;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.OrderItem;
import com.onlinestore.order.grpc.BatchAvailabilityRequest;
import com.onlinestore.order.grpc.ProductAvailabilityRequest;
import com.onlinestore.order.grpc.ProductAvailabilityResponse;

@Component
public class OrderItemMapper {
	public OrderItem toOrderItem(Order order, OrderItemRequest itemRequest, ProductAvailabilityResponse availability) {
		return OrderItem.builder().order(order).productId(itemRequest.getProductId())
				.productName(availability.getProductName()).quantity(itemRequest.getQuantity())
				.price(BigDecimal.valueOf(availability.getPrice())).sale(BigDecimal.valueOf(availability.getDiscount()))
				.build();
	}

	public BatchAvailabilityRequest toBatchAvailabilityRequest(List<OrderItemRequest> itemRequest) {
		List<ProductAvailabilityRequest> availabilityList = new ArrayList<>();
		for (OrderItemRequest request : itemRequest) {
			ProductAvailabilityRequest productAvailabilityRequest = toProductAvailabilityRequest(request);
			availabilityList.add(productAvailabilityRequest);
		}
		return BatchAvailabilityRequest.newBuilder().addAllRequests(availabilityList).build();

	}

	public ProductAvailabilityRequest toProductAvailabilityRequest(OrderItemRequest itemRequest) {
		return ProductAvailabilityRequest.newBuilder().setProductId(String.valueOf(itemRequest.getProductId()))
				.setRequestedQuantity(itemRequest.getQuantity()).build();

	}
}
