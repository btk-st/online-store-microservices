package com.onlinestore.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onlinestore.notification.controller.api.OrderApi;
import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderApi {
	private final OrderService orderService;

	@GetMapping("/all")
	@Override
	public List<OrderDto> getAllOrders() {
		return orderService.getAllOrders();
	}

	@GetMapping("/order/{orderId}")
	@Override
	public OrderDto getItemsByOrderId(@PathVariable UUID orderId) {
		return orderService.getItemsByOrderId(orderId);
	}

	@GetMapping("/user/{userId}")
	@Override
	public List<OrderDto> getOrdersByUserId(@PathVariable UUID userId) {
		return orderService.getOrdersByUserId(userId);
	}
}
