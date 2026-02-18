package com.onlinestore.order.tracing;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Утилитный класс для генерации trace и span id.
 */
@Component
public class TracingUtil {

	/**
	 * Генерирует spanId формата "type-a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
	 * 
	 * @param type
	 *            тип span'a
	 * @return spanId
	 */
	public String generateSpanId(String type) {
		return String.format("%s-%s", type, UUID.randomUUID().toString().substring(0, 8));
	}

	/**
	 * Генерирует traceId
	 * 
	 * @return randomUUID
	 */
	public String generateTraceId() {
		return UUID.randomUUID().toString();
	}
}
