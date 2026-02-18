package com.onlinestore.order.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.OutboxEvent;
import com.onlinestore.order.kafka.OrderCreatedEvent;
import com.onlinestore.order.mapper.OrderMapper;
import com.onlinestore.order.repository.OutboxEventRepository;
import com.onlinestore.order.service.api.TransactionalOutboxService;
import com.onlinestore.order.tracing.TracingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionalOutboxServiceImpl implements TransactionalOutboxService {

	private final OutboxEventRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
	private final OrderMapper orderMapper;
	private final TracingUtil tracingUtil;

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * <b>Особенности реализации:</b>
	 * <ul>
	 * <li>Использует OrderMapper для конвертации Order → OrderCreatedEvent</li>
	 * <li>Сохраняет событие со статусом PENDING и retryCount = 0</li>
	 * <li>Транзакция гарантирует, что событие сохранится вместе с заказом</li>
	 * </ul>
	 * </p>
	 */
	@Override
	public void saveOrderCreatedEvent(Order order) {
		OrderCreatedEvent event = orderMapper.toOrderCreatedEvent(order);

		try {
			String payload = objectMapper.writeValueAsString(event);

			OutboxEvent outboxEvent = OutboxEvent.builder().payload(payload).status(OutboxEvent.EventStatus.PENDING)
					.retryCount(0).build();

			outboxRepository.save(outboxEvent);

		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize event", e);
		}
	}

	/**
	 * Обрабатывает outbox события с повторными попытками.
	 * <p>
	 * Логика обработки:
	 * <ul>
	 * <li>Находит все PENDING события</li>
	 * <li>Пытается отправить каждое в Kafka</li>
	 * <li>При успехе → статус PROCESSED</li>
	 * <li>При ошибке → увеличивает retryCount</li>
	 * <li>После 3 ошибок → статус FAILED</li>
	 * </ul>
	 * </p>
	 */
	// TODO: добавить параллельную обработку. Сейчас только блокирующие вызовы
	@Scheduled(fixedDelay = 10000) // Каждые 10 секунд
	public void processOutboxEvents() {
		List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxEvent.EventStatus.PENDING);

		for (OutboxEvent event : pendingEvents) {
			try {
				sendEventToKafka(event);
				event.setStatus(OutboxEvent.EventStatus.PROCESSED);
			} catch (Exception e) {
				event.setRetryCount(event.getRetryCount() + 1);
				if (event.getRetryCount() >= 3) {
					event.setStatus(OutboxEvent.EventStatus.FAILED);
				}
			}
			outboxRepository.save(event);
		}
	}

	/**
	 * Отправляет конкретное outbox событие в Kafka.
	 *
	 * @param event
	 *            outbox событие для отправки
	 * @throws RuntimeException
	 *             если:
	 *             <ul>
	 *             <li>десериализация JSON не удалась</li>
	 *             <li>Kafka недоступна (timeout 5 сек)</li>
	 *             <li>другая ошибка при отправке</li>
	 *             </ul>
	 */
	private void sendEventToKafka(OutboxEvent event) {
		try {
			// Десериализуй из JSON в OrderCreatedEvent
			OrderCreatedEvent orderEvent = objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);

			String orderId = orderEvent.getOrderId().toString();

			// Добавляем traceId, spanId
			addTracing();
			// Отправляем в Kafka
			kafkaTemplate.send("orders", orderId, orderEvent).get(5, TimeUnit.SECONDS); // Блокируем для надежности

			log.info("Outbox event {} sent to Kafka", event.getId());

		} catch (Exception e) {
			log.error("Failed to send outbox event {} to Kafka", event.getId(), e);
			throw new RuntimeException("Failed to send to Kafka", e);
		}
	}

	/**
	 * Добавляет трассировочную информацию в MDC для логирования. Используется для
	 * связывания логов outbox обработчика с исходным запросом.
	 */
	private void addTracing() {
		MDC.put("traceId", tracingUtil.generateTraceId());
		MDC.put("spanId", tracingUtil.generateSpanId("outbox"));
	}
}
