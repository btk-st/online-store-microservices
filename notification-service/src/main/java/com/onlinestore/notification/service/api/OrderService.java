package com.onlinestore.notification.service.api;

import java.util.List;
import java.util.UUID;

import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.kafka.OrderCreatedEvent;

public interface OrderService {
	/**
	 * Возвращает список всех заказов из системы.
	 * 
	 * @return список всех заказов (может быть пустым)
	 */
	List<OrderDto> getAllOrders();

	/**
	 * Получает заказ по ID с использованием кеширования.
	 * 
	 * @param orderId
	 *            UUID заказа
	 * @return DTO заказа или null, если заказ не найден
	 */
	OrderDto getItemsByOrderId(UUID orderId);

	/**
	 * Возвращает все заказы конкретного пользователя.
	 * 
	 * @param userId
	 *            UUID пользователя
	 * @return список заказов пользователя
	 */
	List<OrderDto> getOrdersByUserId(UUID userId);
	/**
	 * Обрабатывает событие о создании нового заказа из Kafka.
	 * 
	 * @param event
	 *            событие с данными о заказе
	 */
	void processOrderEvent(OrderCreatedEvent event);
}
