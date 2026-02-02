package com.onlinestore.order.grpc;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.onlinestore.order.exception.InventoryServiceException;
import com.onlinestore.order.exception.ProductNotAvailableException;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryGrpcClient {

	@GrpcClient("inventory-service")
	private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

	/**
	 * Проверяет доступность товара в Inventory Service
	 *
	 * @param productId
	 *            ID товара
	 * @param quantity
	 *            запрашиваемое количество
	 * @return информация о товаре и его доступности
	 * @throws ProductNotAvailableException
	 *             если товар недоступен
	 */
	public ProductAvailabilityResponse checkAvailability(UUID productId, int quantity) {
		log.info("Checking availability for product: {}, quantity: {}", productId, quantity);

		try {
			ProductAvailabilityRequest request = ProductAvailabilityRequest.newBuilder()
					.setProductId(productId.toString()).setRequestedQuantity(quantity).build();

			ProductAvailabilityResponse response = inventoryStub.checkAvailability(request);

			log.debug("Received gRPC response for product {}: available={}, quantity={}", productId,
					response.getIsAvailable(), response.getAvailableQuantity());

			return response;

		} catch (StatusRuntimeException e) {
			log.error("gRPC call failed for product {}: {}", productId, e.getStatus());
			throw new InventoryServiceException(
					"Failed to check product availability: " + e.getStatus().getDescription(), e);
		}
	}

	/**
	 * Проверяет доступность товара и выбрасывает исключение если недоступен
	 */
	public ProductAvailabilityResponse checkAvailabilityOrThrow(UUID productId, int quantity) {
		ProductAvailabilityResponse response = checkAvailability(productId, quantity);

		if (!response.getIsAvailable()) {
			throw new ProductNotAvailableException(productId, quantity, response.getAvailableQuantity(),
					response.getMessage());
		}

		return response;
	}
}
