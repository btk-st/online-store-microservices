package com.onlinestore.inventory.tracing;

import java.util.UUID;

import org.slf4j.MDC;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC интерсептор для распределенной трассировки запросов.
 * <p>
 * Добавляет сквозную трассировку путем передачи traceId через метаданные gRPC.
 * Логика работы:
 * <ol>
 * <li>Извлекает traceId из метаданных (переданный от клиента)</li>
 * <li>Если traceId отсутствует (прямой вызов), генерирует новый</li>
 * <li>Генерирует spanId для текущего вызова</li>
 * <li>Устанавливает все ID в MDC для логирования</li>
 * <li>Очищает MDC после завершения вызова (onComplete/onCancel)</li>
 * </ol>
 * </p>
 *
 * <p>
 * Ожидаемые метаданные от клиента:
 * <ul>
 * <li>{@code traceId} - идентификатор всей цепочки вызовов</li>
 * <li>{@code spanId} - идентификатор предыдущего вызова (parentSpanId)</li>
 * </ul>
 * </p>
 */
@Slf4j
public class GrpcServerTracingInterceptor implements ServerInterceptor {
	/**
	 * Перехватывает gRPC вызов и добавляет трассировочную информацию.
	 * 
	 * @param serverCall
	 *            объект вызова
	 * @param metadata
	 *            метаданные запроса
	 * @param serverCallHandler
	 *            обработчик вызова
	 * @return listener с очисткой MDC после завершения
	 */
	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
			ServerCallHandler<ReqT, RespT> serverCallHandler) {
		String traceId = metadata.get(Metadata.Key.of("traceId", Metadata.ASCII_STRING_MARSHALLER));
		String clientSpanId = metadata.get(Metadata.Key.of("spanId", Metadata.ASCII_STRING_MARSHALLER));
		log.info("server intercept traceId={}, clientSpanId={}", traceId, clientSpanId);
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
			log.debug("Generated new traceId for direct gRPC call: {}", traceId);
		}

		String spanId = generateSpanId("grpc");

		MDC.put("traceId", traceId);
		MDC.put("spanId", spanId);
		MDC.put("parentSpanId", clientSpanId);

		ServerCall.Listener<ReqT> originalListener = serverCallHandler.startCall(serverCall, metadata);

		return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(originalListener) {
			@Override
			public void onComplete() {
				try {
					super.onComplete();
				} finally {
					MDC.clear();
				}
			}

			@Override
			public void onCancel() {
				try {
					super.onCancel();
				} finally {
					MDC.clear();
				}
			}
		};
	}

	private String generateSpanId(String type) {
		return String.format("%s-%s", type, UUID.randomUUID().toString().substring(0, 8));
	}
}
