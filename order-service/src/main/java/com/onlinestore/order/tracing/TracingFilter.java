package com.onlinestore.order.tracing;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TracingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = request.getHeader("traceId");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}
		String spanId = generateSpanId("http");
		String parentSpanId = request.getHeader("spanId");

		MDC.put("traceId", traceId);
		MDC.put("spanId", spanId);
		MDC.put("parentSpanId", parentSpanId);

		response.setHeader("traceId", traceId);
		response.setHeader("spanId", spanId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}

	}
	private String generateSpanId(String type) {
		return String.format("%s-%s", type, UUID.randomUUID().toString().substring(0, 8));
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
	}
}
