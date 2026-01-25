package com.onlinestore.order.service;

import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.OrderItem;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.exception.OrderNotFoundException;
import com.onlinestore.order.exception.ProductNotAvailableException;
import com.onlinestore.order.grpc.InventoryGrpcClient;
import com.onlinestore.order.kafka.OrderKafkaProducer;
import com.onlinestore.order.repository.OrderRepository;
import com.onlinestore.order.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryGrpcClient inventoryClient;
    private final TransactionalOutboxService transactionalOutboxService;
    private final OrderKafkaProducer orderKafkaProducer;

    @Transactional
    public Order createOrder(UUID userId, CreateOrderRequest request) {
        log.info("Creating order for user: {}", userId);

        // 1. Находим пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. Проверяем что userId в запросе совпадает с аутентифицированным
        if (!userId.equals(request.getUserId())) {
            throw new IllegalArgumentException("User ID mismatch");
        }

        // 3. Создаем заказ
        Order order = Order.builder()
                .user(user)
                .build();

        // 4. Добавляем товары с проверкой доступности
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            // Проверяем наличие через gRPC
            var availability = inventoryClient.checkAvailabilityOrThrow(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity());

            // Создаем OrderItem с актуальной информацией
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(itemRequest.getProductId())
                    .productName(availability.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .price(BigDecimal.valueOf(availability.getPrice()))
                    .sale(BigDecimal.valueOf(availability.getDiscount()))
                    .build();

            order.addItem(orderItem);
        }


        // 6. Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        // 7. Отправляем в аутбокс
        transactionalOutboxService.saveOrderCreatedEvent(savedOrder);

        //8. Отправляем в кафку (мб с ошибкой, заказ все равно будет сделан + сохранен в аутбокс
        orderKafkaProducer.sendOrderCreated(savedOrder);

        log.info("Order created successfully: {}", savedOrder.getId());
        return savedOrder;
    }
}
