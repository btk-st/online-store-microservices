package com.onlinestore.order.tracing;

import org.slf4j.MDC;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC клиентский интерсептор для распределенной трассировки запросов.
 * <p>
 * Работает в паре с GrpcServerTracingInterceptor на стороне сервера.
 * Автоматически добавляет заголовки трассировки из MDC контекста в исходящие
 * gRPC вызовы.
 * </p>
 *
 * <h3>Логика работы:</h3>
 * <ol>
 * <li>Перехватывает каждый исходящий gRPC вызов</li>
 * <li>Извлекает traceId и spanId из MDC (должны быть установлены ранее,
 * например, {@link TracingFilter})</li>
 * <li>Добавляет их в метаданные запроса</li>
 * <li>Серверный интерсептор GrpcServerTracingInterceptor извлечет эти
 * заголовки</li>
 * </ol>
 *
 * <h3>Добавляемые заголовки:</h3>
 * <ul>
 * <li>{@code traceId} - идентификатор всей цепочки вызовов</li>
 * <li>{@code spanId} - идентификатор текущего span (станет parentSpanId на
 * сервере)</li>
 * </ul>
 *
 * @see TracingFilter
 * @see org.slf4j.MDC
 */
@Slf4j
public class GrpcClientTracingInterceptor implements ClientInterceptor {

	/**
	 * Перехватывает gRPC вызов и добавляет заголовки трассировки.
	 *
	 * @param method
	 *            вызываемый метод
	 * @param callOptions
	 *            опции вызова
	 * @param next
	 *            следующий обработчик в цепочке
	 * @return обернутый вызов с добавлением заголовков при старте
	 */
	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
			CallOptions callOptions, Channel next) {

		log.debug("Adding tracing to gRPC call: {}", method.getFullMethodName());

		return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

			@Override
			public void start(Listener<RespT> responseListener, Metadata headers) {
				// Автоматически добавляем текущий traceId и spanId
				injectTracingHeaders(headers);
				super.start(responseListener, headers);
			}
		};
	}

	/**
	 * Внедряет заголовки трассировки из MDC в метаданные gRPC запроса.
	 * <p>
	 * Если в MDC есть traceId и spanId (установлены в HTTP фильтре или другом
	 * месте), они добавляются в заголовки. Если значений нет, вызов продолжается
	 * без них.
	 * </p>
	 *
	 * @param headers
	 *            метаданные gRPC запроса для модификации
	 */
	private void injectTracingHeaders(Metadata headers) {
		String traceId = MDC.get("traceId");
		String spanId = MDC.get("spanId");

		if (traceId != null) {
			headers.put(Metadata.Key.of("traceId", Metadata.ASCII_STRING_MARSHALLER), traceId);
			log.trace("Added traceId to gRPC headers: {}", traceId);
		}

		if (spanId != null) {
			headers.put(Metadata.Key.of("spanId", Metadata.ASCII_STRING_MARSHALLER), spanId);
			log.trace("Added spanId to gRPC headers: {}", spanId);
		}
	}
}
