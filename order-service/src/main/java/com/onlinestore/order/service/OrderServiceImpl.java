package com.onlinestore.order.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.dto.CreateOrderRequest.OrderItemRequest;
import com.onlinestore.order.dto.OrderResponse;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.OrderItem;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.exception.ProductNotAvailableException;
import com.onlinestore.order.grpc.BatchAvailabilityResponse;
import com.onlinestore.order.grpc.InventoryGrpcClient;
import com.onlinestore.order.grpc.ProductAvailabilityResponse;
import com.onlinestore.order.kafka.OrderKafkaProducer;
import com.onlinestore.order.mapper.OrderItemMapper;
import com.onlinestore.order.mapper.OrderMapper;
import com.onlinestore.order.repository.OrderRepository;
import com.onlinestore.order.repository.UserRepository;
import com.onlinestore.order.service.api.OrderService;
import com.onlinestore.order.service.api.TransactionalOutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final InventoryGrpcClient inventoryClient;
	private final TransactionalOutboxService transactionalOutboxService;
	private final OrderKafkaProducer orderKafkaProducer;
	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;

	/**
	 * Создает новый заказ для пользователя.
	 * <p>
	 * Процесс создания заказа:
	 * <ol>
	 * <li><b>Валидация пользователя</b> - проверка существования и соответствия
	 * ID</li>
	 * <li><b>Проверка наличия товаров</b> - через gRPC запрос в Inventory
	 * сервис</li>
	 * <li><b>Создание заказа</b> - только если ВСЕ товары доступны</li>
	 * <li><b>Сохранение в БД</b> - в рамках одной транзакции</li>
	 * <li><b>Отправка событий</b> - в outbox таблицу и Kafka (best effort)</li>
	 * </ol>
	 * </p>
	 *
	 * <h3>Важные особенности:</h3>
	 * <ul>
	 * <li><b>Атомарность:</b> Либо создается весь заказ, либо ничего
	 * (транзакционно)</li>
	 * <li><b>Проверка наличия:</b> Соответствие между индексами запроса и ответа
	 * gRPC обязательно</li>
	 * <li><b>Обработка ошибок:</b> Если хотя бы один товар недоступен - заказ НЕ
	 * создается</li>
	 * <li><b>События:</b> OrderCreatedEvent сохраняется в outbox и отправляется в
	 * Kafka</li>
	 * <li><b>Отказоустойчивость:</b> Ошибка Kafka НЕ откатывает транзакцию (заказ
	 * сохраняется)</li>
	 * </ul>
	 *
	 * @param userId
	 *            ID аутентифицированного пользователя (из SecurityContext)
	 * @param request
	 *            DTO с составом заказа (список товаров и их количество)
	 * @return созданный заказ с присвоенным ID и рассчитанной общей стоимостью
	 * @throws IllegalArgumentException
	 *             если:
	 *             <ul>
	 *             <li>пользователь не найден</li>
	 *             <li>userId в запросе не совпадает с аутентифицированным</li>
	 *             </ul>
	 * @throws ProductNotAvailableException
	 *             если хотя бы один товар недоступен в запрошенном количестве
	 *
	 * @see Order
	 * @see InventoryGrpcClient#batchCheckAvailability
	 * @see TransactionalOutboxService#saveOrderCreatedEvent
	 * @see OrderKafkaProducer#sendOrderCreated
	 */
	@Transactional
	@Override
	public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
		log.info("Creating order for user: {}", userId);

		// Находим пользователя
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		// Проверяем что userId в запросе совпадает с аутентифицированным
		if (!userId.equals(request.getUserId())) {
			throw new IllegalArgumentException("User ID mismatch");
		}

		// Создаем заказ
		Order order = Order.builder().user(user).build();

		BatchAvailabilityResponse response = inventoryClient.batchCheckAvailability(request);

		// полагаемся на очередность итемов запроса = очередность ответа
		for (int i = 0; i < response.getResponsesCount(); i++) {
			ProductAvailabilityResponse curResponse = response.getResponses(i);
			OrderItemRequest curItemRequest = request.getItems().get(i);
			if (!curResponse.getIsAvailable()) {
				throw new ProductNotAvailableException(UUID.fromString(curResponse.getProductId()),
						curItemRequest.getQuantity(), curResponse.getAvailableQuantity(), curResponse.getMessage());
			}
			OrderItem orderItem = orderItemMapper.toOrderItem(order, curItemRequest, curResponse);
			order.addItem(orderItem);
		}

		// Сохраняем заказ
		Order savedOrder = orderRepository.save(order);

		// Отправляем в аутбокс
		transactionalOutboxService.saveOrderCreatedEvent(savedOrder);

		// Отправляем в кафку (мб с ошибкой, заказ все равно будет сделан + сохранен в
		// аутбокс
		orderKafkaProducer.sendOrderCreated(savedOrder);

		log.info("Order created successfully: {}", savedOrder.getId());
		return orderMapper.toOrderResponse(savedOrder);
	}
}
