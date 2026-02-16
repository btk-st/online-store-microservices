package com.onlinestore.inventory.tracing;

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

/**
 * Servlet фильтр для распределенной трассировки HTTP запросов.
 * <p>
 * Работает в связке с {@link GrpcServerTracingInterceptor} для обеспечения
 * сквозной трассировки через все сервисы.
 * </p>
 *
 * <h3>Логика работы:</h3>
 * <ol>
 * <li>Извлекает traceId из HTTP заголовка "traceId" (если есть)</li>
 * <li>Если traceId отсутствует - генерирует новый UUID</li>
 * <li>Генерирует spanId для текущего HTTP вызова</li>
 * <li>Сохраняет parentSpanId из заголовка "spanId" (если есть)</li>
 * <li>Устанавливает все ID в MDC для логирования</li>
 * <li>Прокидывает traceId и spanId в ответные заголовки</li>
 * <li>Очищает MDC после завершения запроса</li>
 * </ol>
 *
 * <h3>Заголовки запроса (входящие):</h3>
 * <ul>
 * <li>{@code traceId} - идентификатор всей цепочки вызовов</li>
 * <li>{@code spanId} - идентификатор предыдущего вызова (parent)</li>
 * </ul>
 *
 * <h3>Заголовки ответа (исходящие):</h3>
 * <ul>
 * <li>{@code traceId} - тот же traceId, что пришел в запросе</li>
 * <li>{@code spanId} - сгенерированный spanId текущего запроса</li>
 * </ul>
 *
 * <h3>Исключенные пути (не трассируются):</h3>
 * <ul>
 * <li>/actuator/** - эндпоинты Spring Boot Actuator</li>
 * <li>/swagger/** - Swagger UI</li>
 * <li>/v3/api-docs - OpenAPI спецификация</li>
 * </ul>
 */
@Slf4j
@Component
public class TracingFilter extends OncePerRequestFilter {

	/**
	 * Обрабатывает входящий HTTP запрос, добавляя трассировочную информацию.
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

	/**
	 * Определяет, нужно ли пропустить фильтр для данного запроса. Исключаются пути
	 * мониторинга и документации, чтобы не засорять трассировку.
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
