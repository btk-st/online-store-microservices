package com.onlinestore.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.entity.OrderEntity;
import com.onlinestore.notification.kafka.OrderCreatedEvent;
import com.onlinestore.notification.kafka.OrderCreatedEvent.OrderItemEvent;
import com.onlinestore.notification.mapper.OrderMapper;
import com.onlinestore.notification.repository.OrderRepository;
import com.onlinestore.notification.service.api.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
	private final OrderRepository orderRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final OrderMapper orderMapper;

	@Override
	public List<OrderDto> getAllOrders() {
		List<OrderDto> orders = new ArrayList<>();
		var allOrderIds = orderRepository.findAllOrderIds();
		for (UUID orderId : allOrderIds) {
			var order = getItemsByOrderId(orderId);
			if (order != null) {
				orders.add(order);
			}
		}
		return orders;
	}

	@Override
	public OrderDto getItemsByOrderId(UUID orderId) {
		String key = "order:" + orderId;
		OrderDto cached = (OrderDto) redisTemplate.opsForValue().get(key);
		if (cached != null) {
			return cached;
		}
		// Если не нашли в кеше
		List<OrderEntity> orders = orderRepository.findByOrderId(orderId);
		if (orders.isEmpty()) {
			return null;
		}
		var orderDto = orderMapper.toOrderDto(orders);
		// Сохраняем в кеш
		redisTemplate.opsForValue().set(key, orderDto);
		return orderDto;
	}

	@Override
	public List<OrderDto> getOrdersByUserId(UUID userId) {
		List<OrderDto> orders = new ArrayList<>();
		var orderIdsByUserId = orderRepository.findOrderIdsByUserId(userId);
		for (UUID orderId : orderIdsByUserId) {
			var order = getItemsByOrderId(orderId);
			if (order != null) {
				orders.add(order);
			}
		}
		return orders;
	}

	@Override
	@Transactional
	public void processOrderEvent(OrderCreatedEvent event) {
		// Проверяем ВСЕ items на дубликаты ПЕРЕД сохранением
		boolean hasDuplicate = false;
		for (OrderItemEvent item : event.getItems()) {
			if (orderRepository.existsByOrderIdAndProductId(event.getOrderId(), item.getProductId())) {
				log.info("Duplicate found, skipping this kafka message: order={}, product={}", event.getOrderId(),
						item.getProductId());
				hasDuplicate = true;
				break;
			}
		}

		if (hasDuplicate) {
			return;
		}

		for (OrderItemEvent item : event.getItems()) {
			OrderEntity orderEntity = orderMapper.toOrderEntity(item, event.getOrderId(), event.getUserId());
			orderRepository.save(orderEntity);
		}

		log.info("Order {} saved with {} items", event.getOrderId(), event.getItems().size());
	}
}
