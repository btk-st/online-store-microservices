package com.onlinestore.notification.service;

import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.dto.OrderDto.OrderItemDto;
import com.onlinestore.notification.entity.OrderEntity;
import com.onlinestore.notification.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.parsers.ReturnTypeParser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    public List<OrderDto> getAllOrders() {
        // Группируем все записи по order_id
        Map<UUID, List<OrderEntity>> groupedOrders = orderRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(OrderEntity::getOrderId));

        // Преобразуем в список DTO
        return groupedOrders.entrySet().stream()
                .map(entry -> mapToOrderDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public OrderDto getItemsByOrderId(UUID orderId) {
        List<OrderEntity> orders = orderRepository.findByOrderId(orderId);
        if (orders.isEmpty()) {
            return null;
        }
        return mapToOrderDto(orderId, orders);
    }

    public List<OrderDto> getOrdersByUserId(UUID userId) {
        // Находим все записи пользователя и группируем по order_id
        Map<UUID, List<OrderEntity>> userOrdersGrouped = orderRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.groupingBy(OrderEntity::getOrderId));

        return userOrdersGrouped.entrySet().stream()
                .map(entry -> mapToOrderDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private OrderDto mapToOrderDto(UUID orderId, List<OrderEntity> orderEntities) {
        if (orderEntities.isEmpty()) {
            return null;
        }

        // Берем userId из первой записи (у всех в группе одинаковый)
        UUID userId = orderEntities.get(0).getUserId();

        // Мапим items
        List<OrderItemDto> items = orderEntities.stream()
                .map(this::mapToItemDto)
                .collect(Collectors.toList());


        OrderDto order = OrderDto.builder()
                .orderId(orderId)
                .userId(userId)
                .items(items)
                .build();
        
        //Считаем итоговую сумму
        order.setTotalPrice(order.calculateTotalPrice());
        
        return order;
    }

    private OrderItemDto mapToItemDto(OrderEntity entity) {
        return OrderItemDto.builder()
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .price(entity.getPrice())
                .sale(entity.getSale())
                .totalPrice(entity.getTotalPrice())
                .build();
    }
}
