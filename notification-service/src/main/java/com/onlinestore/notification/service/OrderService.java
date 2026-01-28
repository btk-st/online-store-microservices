package com.onlinestore.notification.service;

import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.dto.OrderDto.OrderItemDto;
import com.onlinestore.notification.entity.OrderEntity;
import com.onlinestore.notification.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.parsers.ReturnTypeParser;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<OrderDto> getAllOrders() {
        List<OrderDto> orders = new ArrayList<>();
        var allOrderIds = orderRepository.findAllOrderIds();
        for (UUID orderId: allOrderIds) {
            var order = getItemsByOrderId(orderId);
            if (order != null) {
                orders.add(order);
            }
        }
        return orders;
    }

    public OrderDto getItemsByOrderId(UUID orderId) {
        String key = "order:" + orderId;
        OrderDto cached = (OrderDto) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        //Если не нашли в кеше
        List<OrderEntity> orders = orderRepository.findByOrderId(orderId);
        if (orders.isEmpty()) {
            return null;
        }
        var orderDto = mapToOrderDto(orderId, orders);
        //Сохраняем в кеш
        redisTemplate.opsForValue().set(key, orderDto);
        return orderDto;
    }

    public List<OrderDto> getOrdersByUserId(UUID userId) {
        List<OrderDto> orders = new ArrayList<>();
        var orderIdsByUserId = orderRepository.findOrderIdsByUserId(userId);
        for (UUID orderId: orderIdsByUserId) {
            var order = getItemsByOrderId(orderId);
            if (order != null) {
                orders.add(order);
            }
        }
        return orders;
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
