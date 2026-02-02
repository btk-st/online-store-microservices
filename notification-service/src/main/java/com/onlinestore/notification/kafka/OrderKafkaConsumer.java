package com.onlinestore.notification.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
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
	public void consume(OrderCreatedEvent event, Acknowledgment acknowledgment) {
		log.info("Received order: {}", event.getOrderId());
		orderService.processOrderEvent(event);
		acknowledgment.acknowledge();
	}
}
