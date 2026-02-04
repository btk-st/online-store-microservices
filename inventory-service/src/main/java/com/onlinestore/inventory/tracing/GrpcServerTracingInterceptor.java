package com.onlinestore.inventory.tracing;

import java.util.UUID;

import org.slf4j.MDC;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcServerTracingInterceptor implements ServerInterceptor {

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
