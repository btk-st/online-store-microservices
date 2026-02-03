package com.onlinestore.inventory.grpc;

import java.util.UUID;

import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.service.api.ProductService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

	private final ProductService productService;
	private final ProductMapper productMapper;

	@Override
	public void checkAvailability(ProductAvailabilityRequest request,
			StreamObserver<ProductAvailabilityResponse> responseObserver) {

		try {
			UUID productId = UUID.fromString(request.getProductId());
			int requestedQuantity = request.getRequestedQuantity();
            
            log.info("Checking availability of product with id {} ", productId);
            
			// Получаем товар
			Product product = productService.getProductEntityById(productId);
			ProductAvailabilityResponse response = productMapper.toAvailabilityResponse(product);

			// Проверяем наличие
			int availableQuantity = product.getQuantity();
			boolean isAvailable = availableQuantity >= requestedQuantity;

			// Формируем ответ
			response = response.toBuilder().setIsAvailable(isAvailable)
					.setMessage(isAvailable ? "Available" : "Insufficient stock").build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (ProductNotFoundException | IllegalArgumentException e) {
			// Товар не найден или uuid неверный формат
			ProductAvailabilityResponse response = productMapper.toFailedAvailabilityResponse(request.getProductId());

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (Exception e) {
			log.error("gRPC error", e);
			responseObserver.onError(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
		}
	}
}
