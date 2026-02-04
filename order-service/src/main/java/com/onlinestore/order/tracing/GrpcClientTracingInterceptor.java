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

@Slf4j
public class GrpcClientTracingInterceptor implements ClientInterceptor {

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
