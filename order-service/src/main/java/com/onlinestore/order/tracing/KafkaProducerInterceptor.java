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

@Slf4j
public class KafkaProducerInterceptor implements ProducerInterceptor<String, OrderCreatedEvent> {

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

	private boolean shouldIncludeHeader(String key) {
		return key.startsWith("traceId") || key.startsWith("spanId");
	}

	@Override
	public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {

	}

	@Override
	public void close() {

	}

	@Override
	public void configure(Map<String, ?> map) {

	}
}
