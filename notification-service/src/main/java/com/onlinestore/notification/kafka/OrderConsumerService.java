package com.onlinestore.notification.kafka;

import com.onlinestore.notification.entity.OrderEntity;
import com.onlinestore.notification.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderConsumerService {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "orders", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        log.info("Received order: {}", event.getOrderId());


        // Проверяем ВСЕ items на дубликаты ПЕРЕД сохранением
        boolean hasDuplicate = false;
        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            if (orderRepository.existsByOrderIdAndProductId(
                    event.getOrderId(), item.getProductId())) {
                log.info("Duplicate found, skipping this kafka message: order={}, product={}",
                        event.getOrderId(), item.getProductId());
                hasDuplicate = true;
                break;
            }
        }

        if (hasDuplicate) {
            acknowledgment.acknowledge();
            return;
        }

        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {

            BigDecimal discountMultiplier = BigDecimal.ONE
                    .subtract(item.getSale()
                            .divide(BigDecimal.valueOf(100)));

            BigDecimal itemTotalPrice = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .multiply(discountMultiplier);

            OrderEntity orderEntity = OrderEntity.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .sale(item.getSale())
                    .totalPrice(itemTotalPrice)
                    .build();

            orderRepository.save(orderEntity);
        }
        log.info("Order {} saved with {} items",
                    event.getOrderId(), event.getItems().size());

        acknowledgment.acknowledge();
    }
}
