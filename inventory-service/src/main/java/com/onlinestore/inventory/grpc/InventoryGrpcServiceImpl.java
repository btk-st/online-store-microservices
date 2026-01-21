package com.onlinestore.inventory.grpc;

import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.service.ProductService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final ProductService productService;

    @Override
    public void checkAvailability(ProductAvailabilityRequest request,
                                  StreamObserver<ProductAvailabilityResponse> responseObserver) {

        try {
            UUID productId = UUID.fromString(request.getProductId());
            int requestedQuantity = request.getRequestedQuantity();

            // Получаем товар
            Product product = productService.getProductEntityById(productId);

            // Проверяем наличие
            int availableQuantity = product.getQuantity();
            boolean isAvailable = availableQuantity >= requestedQuantity;

            // Считаем цену со скидкой
            double price = product.getPrice().doubleValue();
            double discount = product.getSale() != null ? product.getSale().doubleValue() : 0.0;

            // Формируем ответ
            ProductAvailabilityResponse response = ProductAvailabilityResponse.newBuilder()
                    .setProductId(productId.toString())
                    .setProductName(product.getName())
                    .setPrice(price)
                    .setDiscount(discount)
                    .setAvailableQuantity(availableQuantity)
                    .setIsAvailable(isAvailable)
                    .setMessage(isAvailable ? "Available" : "Insufficient stock")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (ProductNotFoundException e) {
            // Товар не найден
            ProductAvailabilityResponse response = ProductAvailabilityResponse.newBuilder()
                    .setProductId(request.getProductId())
                    .setIsAvailable(false)
                    .setMessage("Product not found")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }
}
