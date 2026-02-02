package com.onlinestore.order.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onlinestore.order.controller.api.OrderApi;
import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.dto.OrderResponse;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderApi {

	private final OrderService orderService;

	@PostMapping
	@Override
	public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal User user,
			@Valid @RequestBody CreateOrderRequest request) {

		OrderResponse response = orderService.createOrder(user.getId(), request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
