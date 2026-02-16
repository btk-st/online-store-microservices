package com.onlinestore.inventory.grpc;

import java.util.ArrayList;
import java.util.List;
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

/**
 * gRPC сервис для проверки наличия товаров на складе. Обрабатывает запросы от
 * других микросервисов через протокол gRPC.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

	private final ProductService productService;
	private final ProductMapper productMapper;

	/**
	 * Проверяет наличие нужного количества одиночного товара на складе
	 * 
	 * @param request
	 *            gRPC запрос, содержащий id товара и требуемое количество.
	 * @param responseObserver
	 *            объект для отправки ответа обратно клиенту
	 */
	@Override
	public void checkAvailability(ProductAvailabilityRequest request,
			StreamObserver<ProductAvailabilityResponse> responseObserver) {

		try {
			log.info("Checking availability of product with id {} ", request.getProductId());

			ProductAvailabilityResponse response = checkSingleProduct(request);

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

	/**
	 * Пакетная проверка наличия нескольких товаров. Для каждого товара в запросе
	 * возвращается отдельный ответ. Если какой-то товар не найден или количество
	 * товара на складе меньше запрашиваемого, в ответе для него будет isAvailable =
	 * false.
	 * 
	 * @param request
	 *            список товаров с требуемым количеством для проверки.
	 * @param responseObserver
	 *            объект для отправки ответа клиенту.
	 */
	@Override
	public void batchCheckAvailability(BatchAvailabilityRequest request,
			StreamObserver<BatchAvailabilityResponse> responseObserver) {
		log.info("Checking availability of batch products");

		List<ProductAvailabilityResponse> responses = new ArrayList<>();
		for (ProductAvailabilityRequest productAvailabilityRequest : request.getRequestsList()) {
			try {
				ProductAvailabilityResponse response = checkSingleProduct(productAvailabilityRequest);
				responses.add(response);
			} catch (ProductNotFoundException | IllegalArgumentException e) {
				ProductAvailabilityResponse response = productMapper
						.toFailedAvailabilityResponse(productAvailabilityRequest.getProductId());
				responses.add(response);
			}
		}

		BatchAvailabilityResponse batchResponse = BatchAvailabilityResponse.newBuilder().addAllResponses(responses)
				.build();

		responseObserver.onNext(batchResponse);
		responseObserver.onCompleted();
	}

	private ProductAvailabilityResponse checkSingleProduct(ProductAvailabilityRequest request) {
		UUID productId = UUID.fromString(request.getProductId());
		int requestedQuantity = request.getRequestedQuantity();

		// Получаем товар
		Product product = productService.getProductEntityById(productId);
		ProductAvailabilityResponse response = productMapper.toAvailabilityResponse(product);

		// Проверяем наличие
		int availableQuantity = product.getQuantity();
		boolean isAvailable = availableQuantity >= requestedQuantity;

		// Формируем ответ
		return response.toBuilder().setIsAvailable(isAvailable)
				.setMessage(isAvailable ? "Available" : "Insufficient stock").build();
	}

}
