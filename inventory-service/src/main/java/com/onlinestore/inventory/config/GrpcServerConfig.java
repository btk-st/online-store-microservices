package com.onlinestore.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.onlinestore.inventory.tracing.GrpcServerTracingInterceptor;

import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;

/**
 * Конфигурация grpc сервера Настраивает interceptor для реализации трейсинга
 * запросов
 */
@Configuration
public class GrpcServerConfig {

	@Bean
	public GrpcServerConfigurer grpcServerConfigurer() {
		return serverBuilder -> {
			serverBuilder.intercept(new GrpcServerTracingInterceptor());
		};
	}
}
