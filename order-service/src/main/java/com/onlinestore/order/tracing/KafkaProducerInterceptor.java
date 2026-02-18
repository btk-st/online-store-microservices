package com.onlinestore.order.tracing;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import com.onlinestore.order.kafka.OrderCreatedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka interceptor для проброса контекста трассировки в заголовки сообщений.
 * <p>
 * Автоматически добавляет traceId и spanId из MDC в заголовки Kafka сообщений,
 * что позволяет сохранять сквозную трассировку при передаче через Kafka.
 * </p>
 *
 * <h3>Логика работы:</h3>
 * <ol>
 * <li>Перехватывает каждое отправляемое Kafka сообщение</li>
 * <li>Копирует текущий MDC контекст (должен быть установлен ранее)</li>
 * <li>Добавляет traceId и spanId в заголовки сообщения</li>
 * <li>Потребитель может извлечь эти заголовки и восстановить контекст</li>
 * </ol>
 *
 * <h3>Добавляемые заголовки:</h3>
 * <ul>
 * <li><b>traceId</b> - идентификатор всей цепочки вызовов</li>
 * <li><b>spanId</b> - идентификатор текущего span</li>
 * <li>Любые другие ключи из MDC, начинающиеся с "traceId" или "spanId"</li>
 * </ul>
 *
 * <h3>Связь с другими компонентами:</h3>
 * <ul>
 * <li>{@link TracingFilter} - устанавливает traceId/spanId в MDC для HTTP
 * запросов</li>
 * <li>{@link GrpcClientTracingInterceptor} - пробрасывает контекст в gRPC</li>
 * <li>Kafka Consumer должен иметь аналогичный interceptor для восстановления
 * контекста</li>
 * </ul>
 *
 * @see org.apache.kafka.clients.producer.ProducerInterceptor
 * @see org.slf4j.MDC
 * @see TracingFilter
 * @see GrpcClientTracingInterceptor
 */
@Slf4j
public class KafkaProducerInterceptor implements ProducerInterceptor<String, OrderCreatedEvent> {

	/**
	 * Перехватывает сообщение перед отправкой в Kafka.
	 * <p>
	 * Копирует текущий MDC контекст в заголовки сообщения. Добавляются только
	 * ключи, начинающиеся с "traceId" или "spanId".
	 * </p>
	 *
	 * @param producerRecord
	 *            исходное сообщение
	 * @return сообщение с добавленными заголовками трассировки
	 */
	@Override
	public ProducerRecord<String, OrderCreatedEvent> onSend(ProducerRecord<String, OrderCreatedEvent> producerRecord) {
		log.info("Interceptor called for topic: {}", producerRecord.topic());
		Map<String, String> mdcContext = MDC.getCopyOfContextMap();

		if (mdcContext != null && !mdcContext.isEmpty()) {
			Headers headers = producerRecord.headers();

			mdcContext.forEach((key, value) -> {
				if (shouldIncludeHeader(key) && value != null) {
					headers.add(key, value.getBytes(StandardCharsets.UTF_8));
				}
			});
		}

		return producerRecord;
	}

	/**
	 * Определяет, нужно ли добавлять ключ из MDC в заголовки.
	 * <p>
	 * Добавляются только ключи, связанные с трассировкой:
	 * <ul>
	 * <li>traceId* - все, что начинается с "traceId"</li>
	 * <li>spanId* - все, что начинается с "spanId"</li>
	 * </ul>
	 * </p>
	 *
	 * @param key
	 *            ключ из MDC
	 * @return true если ключ нужно добавить в заголовки
	 */
	private boolean shouldIncludeHeader(String key) {
		return key.startsWith("traceId") || key.startsWith("spanId");
	}

	/**
	 * Вызывается после подтверждения отправки сообщения брокером. В данной
	 * реализации не используется.
	 *
	 * @param recordMetadata
	 *            метаданные отправленного сообщения
	 * @param e
	 *            ошибка (null если успешно)
	 */
	@Override
	public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {
		// No configuration needed
	}

	/**
	 * Закрывает interceptor при остановке producer.
	 */
	@Override
	public void close() {
		// No configuration needed
	}

	/**
	 * Конфигурирует interceptor при создании.
	 *
	 * @param map
	 *            конфигурационные параметры
	 */
	@Override
	public void configure(Map<String, ?> map) {
		// No configuration needed
	}
}
