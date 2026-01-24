package com.onlinestore.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.OutboxEvent;
import com.onlinestore.order.kafka.OrderCreatedEvent;
import com.onlinestore.order.kafka.OrderKafkaProducer;
import com.onlinestore.order.repository.OrderRepository;
import com.onlinestore.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionalOutboxService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    // Вызывается в той же транзакции что и создание Order
    public void saveOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.from(order);

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(OutboxEvent.EventType.ORDER_CREATED)
                    .aggregateType("ORDER")
                    .aggregateId(order.getId().toString())
                    .payload(payload)
                    .status(OutboxEvent.EventStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxRepository.save(outboxEvent);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    //TODO: добавить параллельную обработку. Сейчас только блокирующие вызовы

    // Периодическая задача отправляет PENDING события
    @Scheduled(fixedDelay = 10000) // Каждые 10 секунд
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
                .findByStatus(OutboxEvent.EventStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                sendEventToKafka(event);
                event.setStatus(OutboxEvent.EventStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxEvent.EventStatus.FAILED);
                }
            }
            outboxRepository.save(event);
        }
    }

    private void sendEventToKafka(OutboxEvent event) {
        try {
            // Десериализуй из JSON в OrderCreatedEvent
            OrderCreatedEvent orderEvent = objectMapper.readValue(
                    event.getPayload(),
                    OrderCreatedEvent.class
            );

            String orderId = orderEvent.getOrderId().toString();
            // Отправляем в Kafka
            kafkaTemplate.send("orders", orderId, orderEvent)
                    .get(5, TimeUnit.SECONDS); // Блокируем для надежности

            log.info("Outbox event {} sent to Kafka", event.getId());

        } catch (Exception e) {
            log.error("Failed to send outbox event {} to Kafka", event.getId(), e);
            throw new RuntimeException("Failed to send to Kafka", e);
        }
    }
}
