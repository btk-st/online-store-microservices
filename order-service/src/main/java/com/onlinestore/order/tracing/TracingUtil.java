package com.onlinestore.order.tracing;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class TracingUtil {

	public String generateSpanId(String type) {
		return String.format("%s-%s", type, UUID.randomUUID().toString().substring(0, 8));
	}

	public String generateTraceId() {
		return UUID.randomUUID().toString();
	}
}
