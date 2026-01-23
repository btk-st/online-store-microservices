package com.onlinestore.order.kafka;

import com.onlinestore.order.entity.Order;
import com.onlinestore.order.service.TransactionalOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;


    public void sendOrderCreated(Order order) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.from(order);
            String orderId = order.getId().toString();

            log.info("Sending OrderCreatedEvent to Kafka, orderId: {}", orderId);

            kafkaTemplate.send("orders", orderId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("OrderCreatedEvent sent successfully, offset: {}",
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send OrderCreatedEvent", ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending to Kafka", e);
        }
    }
}
