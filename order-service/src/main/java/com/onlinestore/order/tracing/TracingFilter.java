package com.onlinestore.order.tracing;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracingFilter extends OncePerRequestFilter {

	private final TracingUtil tracingUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = request.getHeader("traceId");
		if (traceId == null) {
			traceId = tracingUtil.generateTraceId();
		}
		String spanId = tracingUtil.generateSpanId("http");
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

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
	}
}
