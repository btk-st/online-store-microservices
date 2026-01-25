package com.onlinestore.order.kafka;

import com.onlinestore.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private UUID orderId;
    private UUID userId;
    private List<OrderItemEvent> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private UUID productId;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal sale;
    }

    public static OrderCreatedEvent from(Order order) {
        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUser().getId())
                .items(order.getItems().stream()
                        .map(item -> OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .sale(item.getSale())
                                .build())
                        .toList())
                .build();
    }
}
