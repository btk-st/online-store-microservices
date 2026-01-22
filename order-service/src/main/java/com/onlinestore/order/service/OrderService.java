package com.onlinestore.order.service;

import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.OrderItem;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.exception.OrderNotFoundException;
import com.onlinestore.order.exception.ProductNotAvailableException;
import com.onlinestore.order.grpc.InventoryGrpcClient;
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
                .status(Order.OrderStatus.CREATED)
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
                    .unitPrice(BigDecimal.valueOf(availability.getPrice()))
                    .discount(BigDecimal.valueOf(availability.getDiscount()))
                    .build();

            order.addItem(orderItem);
        }

        // 5. Рассчитываем общую сумму
        order.setTotalPrice(order.calculateTotalPrice());

        // 6. Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {}", savedOrder.getId());
        return savedOrder;
    }

    public Order getOrderById(UUID orderId) {
        log.info("Getting order by id: {}", orderId);

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public Order getOrderByIdAndUserId(UUID orderId, UUID userId) {
        log.info("Getting order {} for user {}", orderId, userId);

        return orderRepository.findById(orderId)
                .filter(order -> order.getUser().getId().equals(userId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<Order> getOrdersByUser(UUID userId) {
        log.info("Getting all orders for user: {}", userId);

        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        log.info("Getting all orders");

        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, Order.OrderStatus newStatus) {
        log.info("Updating order {} status to {}", orderId, newStatus);

        Order order = getOrderById(orderId);
        order.setStatus(newStatus);

        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        log.info("Cancelling order {} for user {}", orderId, userId);

        Order order = getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != Order.OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Only orders with CREATED status can be cancelled. Current status: " +
                            order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order {} cancelled successfully", orderId);
    }

    @Transactional
    public void deleteOrder(UUID orderId, UUID userId) {
        log.info("Deleting order {} for user {}", orderId, userId);

        if (!orderRepository.existsByIdAndUserId(orderId, userId)) {
            throw new OrderNotFoundException(orderId);
        }

        orderRepository.deleteById(orderId);
        log.info("Order {} deleted successfully", orderId);
    }
}
