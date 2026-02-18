package com.onlinestore.order.grpc;

import org.springframework.stereotype.Component;

import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.exception.InventoryServiceException;
import com.onlinestore.order.mapper.OrderItemMapper;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

/**
 * gRPC клиент для проверки доступности товара в микросервисе inventory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryGrpcClient {

	@GrpcClient("inventory-service")
	private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;
	private final OrderItemMapper orderItemMapper;

	/**
	 * Пакетная проверка наличия достаточного количества товаров.
	 * 
	 * @param createOrderRequest
	 *            список товаров для проверки
	 * @return список товаров с информацией о доступности
	 */
	public BatchAvailabilityResponse batchCheckAvailability(CreateOrderRequest createOrderRequest) {
		log.info("Checking availability for {} items", createOrderRequest.getItems().size());

		try {
			BatchAvailabilityRequest request = orderItemMapper
					.toBatchAvailabilityRequest(createOrderRequest.getItems());

			BatchAvailabilityResponse response = inventoryStub.batchCheckAvailability(request);

			log.debug("Received gRPC response for {} items", response.getResponsesCount());

			return response;

		} catch (StatusRuntimeException e) {
			throw new InventoryServiceException(
					"Failed to check product availability: " + e.getStatus().getDescription(), e);
		}
	}
}
