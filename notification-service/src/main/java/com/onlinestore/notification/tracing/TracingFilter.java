package com.onlinestore.notification.tracing;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.onlinestore.notification.kafka.OrderKafkaConsumer;

import lombok.extern.slf4j.Slf4j;

/**
 * Servlet фильтр для распределенной трассировки HTTP запросов в Notification
 * микросервисе.
 * <p>
 * Обеспечивает сквозную трассировку через все сервисы.
 * </p>
 *
 * <h3>Логика работы:</h3>
 * <ol>
 * <li>Извлекает traceId из HTTP заголовка "traceId"</li>
 * <li>Если traceId отсутствует (прямой вызов) - генерирует новый UUID</li>
 * <li>Генерирует spanId для текущего HTTP вызова</li>
 * <li>Сохраняет parentSpanId из заголовка "spanId" (если есть)</li>
 * <li>Устанавливает все ID в MDC для логирования</li>
 * <li>Прокидывает traceId и spanId в ответные заголовки</li>
 * <li>Очищает MDC после завершения запроса</li>
 * </ol>
 *
 * <h3>Исключенные пути (не трассируются):</h3>
 * <ul>
 * <li>/actuator/** - эндпоинты Spring Boot Actuator</li>
 * <li>/swagger/** - Swagger UI</li>
 * <li>/v3/api-docs - OpenAPI спецификация</li>
 * </ul>
 *
 * @see OrderKafkaConsumer
 */
@Slf4j
@Component
public class TracingFilter extends OncePerRequestFilter {

	/**
	 * Обрабатывает входящий HTTP запрос, добавляя трассировочную информацию.
	 * <p>
	 * traceId и spanId автоматически добавляются в логи через MDC и передаются в
	 * ответные заголовки для обеспечения сквозной трассировки.
	 * </p>
	 *
	 * @param request
	 *            HTTP запрос
	 * @param response
	 *            HTTP ответ
	 * @param filterChain
	 *            цепочка фильтров
	 * @throws ServletException
	 *             если ошибка сервлета
	 * @throws IOException
	 *             если ошибка ввода-вывода
	 */
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
