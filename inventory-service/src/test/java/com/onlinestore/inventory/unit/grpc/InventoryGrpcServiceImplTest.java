package com.onlinestore.inventory.unit.grpc;

import java.math.BigDecimal;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.grpc.InventoryGrpcServiceImpl;
import com.onlinestore.inventory.grpc.ProductAvailabilityRequest;
import com.onlinestore.inventory.grpc.ProductAvailabilityResponse;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.service.ProductService;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class InventoryGrpcServiceImplTest {

	@Mock
	private ProductService productService;

	@Mock
	private StreamObserver<ProductAvailabilityResponse> responseObserver;

	@Captor
	private ArgumentCaptor<ProductAvailabilityResponse> responseCaptor;

	@Captor
	private ArgumentCaptor<Throwable> errorCaptor;

	@Spy
	private ProductMapper productMapper = new ProductMapper();

	@InjectMocks
	private InventoryGrpcServiceImpl grpcService;

	private UUID productId;
	private Product testProduct;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();
		testProduct = Product.builder().id(productId).name("iPhone 15 Pro").quantity(10)
				.price(new BigDecimal("1299.99")).sale(new BigDecimal("15.50")).build();
	}

	@Test
	void checkAvailability_WhenProductExistsAndQuantityAvailable_ReturnsSuccess() {
		// Arrange
		Mockito.when(productService.getProductEntityById(productId)).thenReturn(testProduct);
		ProductAvailabilityRequest request = ProductAvailabilityRequest.newBuilder().setProductId(productId.toString())
				.setRequestedQuantity(5).build();

		// Act
		grpcService.checkAvailability(request, responseObserver);

		// Assert
		Mockito.verify(responseObserver).onNext(responseCaptor.capture());
		Mockito.verify(responseObserver).onCompleted();
		ProductAvailabilityResponse response = responseCaptor.getValue();

		Assertions.assertThat(response.getProductId()).isEqualTo(productId.toString());
		Assertions.assertThat(response.getProductName()).isEqualTo("iPhone 15 Pro");
		Assertions.assertThat(response.getPrice()).isEqualTo(1299.99);
		Assertions.assertThat(response.getDiscount()).isEqualTo(15.50);
		Assertions.assertThat(response.getAvailableQuantity()).isEqualTo(10);
		Assertions.assertThat(response.getIsAvailable()).isTrue();
		Assertions.assertThat(response.getMessage()).isEqualTo("Available");
	}

	@Test
	void checkAvailability_WhenProductExistsButInsufficientQuantity_ReturnsNotAvailable() {
		// Arrange
		Mockito.when(productService.getProductEntityById(productId)).thenReturn(testProduct);
		ProductAvailabilityRequest request = ProductAvailabilityRequest.newBuilder().setProductId(productId.toString())
				.setRequestedQuantity(15) // больше чем есть
				.build();

		// Act
		grpcService.checkAvailability(request, responseObserver);

		// Assert
		Mockito.verify(responseObserver).onNext(responseCaptor.capture());
		ProductAvailabilityResponse response = responseCaptor.getValue();

		Assertions.assertThat(response.getIsAvailable()).isFalse();
		Assertions.assertThat(response.getMessage()).isEqualTo("Insufficient stock");
		Assertions.assertThat(response.getAvailableQuantity()).isEqualTo(10);
	}

	@Test
	void checkAvailability_WhenProductNotFound_ReturnsNotFound() {
		// Arrange
		Mockito.when(productService.getProductEntityById(Mockito.any(UUID.class)))
				.thenThrow(new ProductNotFoundException(UUID.randomUUID()));
		ProductAvailabilityRequest request = ProductAvailabilityRequest.newBuilder().setProductId(productId.toString())
				.setRequestedQuantity(1).build();

		// Act
		grpcService.checkAvailability(request, responseObserver);

		// Assert
		Mockito.verify(responseObserver).onNext(responseCaptor.capture());
		ProductAvailabilityResponse response = responseCaptor.getValue();

		Assertions.assertThat(response.getProductId()).isEqualTo(productId.toString());
		Assertions.assertThat(response.getIsAvailable()).isFalse();
		Assertions.assertThat(response.getMessage()).isEqualTo("Product not found");
		Assertions.assertThat(response.getAvailableQuantity()).isEqualTo(0);
	}

	@Test
	void checkAvailability_WhenInvalidUUID_ReturnsNotFound() {
		// Arrange
		ProductAvailabilityRequest request = ProductAvailabilityRequest.newBuilder().setProductId("invalid-uuid")
				.setRequestedQuantity(1).build();

		// Act
		grpcService.checkAvailability(request, responseObserver);

		// Assert
		Mockito.verify(responseObserver).onNext(responseCaptor.capture());
		ProductAvailabilityResponse response = responseCaptor.getValue();

		Assertions.assertThat(response.getProductId()).isEqualTo("invalid-uuid");
		Assertions.assertThat(response.getIsAvailable()).isFalse();
		Assertions.assertThat(response.getMessage()).isEqualTo("Product not found");
	}

	@Test
	void checkAvailability_WhenServiceThrowsException_ReturnsInternalError() {
		// Arrange
		Mockito.when(productService.getProductEntityById(Mockito.any(UUID.class)))
				.thenThrow(new RuntimeException("DB error"));
		ProductAvailabilityRequest request = ProductAvailabilityRequest.newBuilder().setProductId(productId.toString())
				.setRequestedQuantity(1).build();

		// Act
		grpcService.checkAvailability(request, responseObserver);

		// Assert
		Mockito.verify(responseObserver).onError(errorCaptor.capture());
		Throwable error = errorCaptor.getValue();

		Assertions.assertThat(error).isInstanceOf(StatusRuntimeException.class);
		Assertions.assertThat(((StatusRuntimeException) error).getStatus().getCode())
				.isEqualTo(io.grpc.Status.Code.INTERNAL);
	}

	@Test
	void checkAvailability_WhenProductHasNoDiscount_ReturnsZeroDiscount() {
		// Arrange
		Product productNoDiscount = Product.builder().id(productId).name("Product").quantity(5)
				.price(new BigDecimal("100.00")).sale(null).build();
		Mockito.when(productService.getProductEntityById(productId)).thenReturn(productNoDiscount);
		ProductAvailabilityRequest request = ProductAvailabilityRequest.newBuilder().setProductId(productId.toString())
				.setRequestedQuantity(2).build();

		// Act
		grpcService.checkAvailability(request, responseObserver);

		// Assert
		Mockito.verify(responseObserver).onNext(responseCaptor.capture());
		Assertions.assertThat(responseCaptor.getValue().getDiscount()).isEqualTo(0.0);
	}
}
