package com.onlinestore.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.onlinestore.order.tracing.GrpcClientTracingInterceptor;

import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;

@Configuration
public class GrpcClientConfig {

	@Bean
	@GrpcGlobalClientInterceptor
	public GrpcClientTracingInterceptor globalTracingInterceptor() {
		return new GrpcClientTracingInterceptor();
	}
}
