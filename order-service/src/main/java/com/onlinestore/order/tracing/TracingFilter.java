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

/**
 * Servlet фильтр для распределенной трассировки HTTP запросов.
 * <p>
 * Является входной точкой для сквозной трассировки в микросервисной
 * архитектуре. Устанавливает traceId и spanId в MDC для логирования и
 * пробрасывает их в исходящие запросы (через заголовки ответа).
 * </p>
 *
 * <h3>Логика работы:</h3>
 * <ol>
 * <li><b>Извлечение traceId</b> - из заголовка "traceId" (если есть) или
 * генерация нового через {@link TracingUtil}</li>
 * <li><b>Генерация spanId</b> - через
 * {@link TracingUtil#generateSpanId(String)} с типом "http"</li>
 * <li><b>Сохранение parentSpanId</b> - из заголовка "spanId" (если есть)</li>
 * <li><b>Установка MDC</b> - traceId, spanId, parentSpanId для логирования</li>
 * <li><b>Прокидывание в ответ</b> - traceId и spanId в заголовки ответа</li>
 * <li><b>Очистка MDC</b> - после завершения запроса (finally блок)</li>
 * </ol>
 *
 * <h3>Заголовки:</h3>
 * <ul>
 * <li><b>Входящие:</b> "traceId", "spanId" (от клиента или предыдущего
 * сервиса)</li>
 * <li><b>Исходящие:</b> "traceId", "spanId" (для следующего сервиса в
 * цепочке)</li>
 * </ul>
 *
 * <h3>Интеграция с другими компонентами:</h3>
 * <ul>
 * <li>{@link TracingUtil} - утилитный класс для генерации идентификаторов</li>
 * <li>{@link GrpcClientTracingInterceptor} - пробрасывает контекст в gRPC
 * вызовы</li>
 * <li>{@link KafkaProducerInterceptor} - пробрасывает контекст в Kafka
 * сообщения</li>
 * </ul>
 *
 * <h3>Исключенные пути (не трассируются):</h3>
 * <ul>
 * <li><b>/actuator/**</b> - эндпоинты Spring Boot Actuator (метрики,
 * healthcheck)</li>
 * <li><b>/swagger/**</b> - Swagger UI документация</li>
 * <li><b>/v3/api-docs</b> - OpenAPI спецификация</li>
 * </ul>
 *
 * @see TracingUtil
 * @see GrpcClientTracingInterceptor
 * @see KafkaProducerInterceptor
 * @see org.slf4j.MDC
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TracingFilter extends OncePerRequestFilter {

	private final TracingUtil tracingUtil;

	/**
	 * Обрабатывает входящий HTTP запрос, добавляя трассировочную информацию.
	 * <p>
	 * <b>Важно:</b> MDC очищается в finally блоке, чтобы избежать утечки контекста
	 * между разными потоками/запросами.
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

	/**
	 * Определяет, нужно ли пропустить фильтр для данного запроса.
	 * <p>
	 * Исключаются системные эндпоинты (actuator, swagger), чтобы:
	 * <ul>
	 * <li>Не засорять трассировку служебными вызовами</li>
	 * <li>Уменьшить нагрузку на MDC</li>
	 * <li>Избежать генерации лишних идентификаторов</li>
	 * </ul>
	 * </p>
	 *
	 * @param request
	 *            HTTP запрос
	 * @return true если фильтр не должен применяться
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
	}
}
