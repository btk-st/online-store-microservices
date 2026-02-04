package com.onlinestore.notification.kafka;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.onlinestore.notification.service.api.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaConsumer {

	private final OrderService orderService;

	@KafkaListener(topics = "orders", groupId = "${spring.kafka.consumer.group-id}")
	public void consume(OrderCreatedEvent event, @Header(value = "traceId", required = false) String traceId,
			@Header(value = "spanId", required = false) String spanId, Acknowledgment acknowledgment) {
		if (traceId != null) {
			MDC.put("traceId", traceId);
		}
		MDC.put("spanId", generateSpanId());
		MDC.put("parentSpanId", spanId);

		try {
			log.info("Received order: {}", event.getOrderId());
			orderService.processOrderEvent(event);
			acknowledgment.acknowledge();
		} catch (Exception e) {
			log.error("Failed to process order: {}", event.getOrderId(), e);
			throw e;
		} finally {
			MDC.clear();
		}
	}

	private String generateSpanId() {
		return String.format("%s-%s", "kafka", UUID.randomUUID().toString().substring(0, 8));
	}
}
