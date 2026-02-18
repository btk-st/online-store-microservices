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

/**
 * Kafka consumer для обработки событий о заказах.
 *
 * <h3>Интеграция с системой трассировки:</h3>
 * <p>
 * Получает traceId и spanId из заголовков Kafka сообщения для обеспечения
 * сквозной трассировки через все микросервисы. Это позволяет связать лог
 * событий в Notification сервисе с исходным HTTP/gRPC запросом.
 * </p>
 *
 * <h3>Обработка ошибок:</h3>
 * <p>
 * При возникновении исключения в процессе обработки события:
 * <ul>
 * <li>Сообщение НЕ подтверждается (acknowledgment не вызывается)</li>
 * <li>Kafka автоматически переотправит сообщение (в зависимости от
 * конфигурации)</li>
 * </ul>
 * </p>
 *
 * <h3>MDC контекст для логирования:</h3>
 * <ul>
 * <li>traceId - из заголовка Kafka (сквозной идентификатор)</li>
 * <li>spanId - генерируется для текущей Kafka операции</li>
 * <li>parentSpanId - spanId отправителя (из заголовка)</li>
 * </ul>
 *
 * @see OrderService#processOrderEvent(OrderCreatedEvent)
 * @see org.springframework.kafka.annotation.KafkaListener
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaConsumer {

	private final OrderService orderService;

	/**
	 * Обрабатывает событие о создании нового заказа.
	 * <p>
	 * Вызывается автоматически при поступлении нового сообщения в топик "orders".
	 * </p>
	 *
	 * @param event
	 *            событие с данными о заказе
	 * @param traceId
	 *            сквозной идентификатор трассировки (из заголовков Kafka)
	 * @param spanId
	 *            идентификатор предыдущего вызова (из заголовков Kafka)
	 * @param acknowledgment
	 *            механизм подтверждения обработки сообщения
	 * @throws Exception
	 *             если обработка не удалась (сообщение будет переотправлено)
	 */
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
