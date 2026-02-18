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
import com.onlinestore.inventory.grpc.BatchAvailabilityRequest;
import com.onlinestore.inventory.grpc.BatchAvailabilityResponse;
import com.onlinestore.inventory.grpc.InventoryGrpcServiceImpl;
import com.onlinestore.inventory.grpc.ProductAvailabilityRequest;
import com.onlinestore.inventory.grpc.ProductAvailabilityResponse;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.service.ProductServiceImpl;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class InventoryGrpcServiceImplTest {

	@Mock
	private ProductServiceImpl productService;

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

	@Test
	void batchCheckAvailability_WhenAllProductsAvailable_ReturnsAllSuccess() {
		// Arrange
		UUID productId1 = UUID.randomUUID();
		UUID productId2 = UUID.randomUUID();

		Product product1 = Product.builder().id(productId1).name("iPhone").quantity(10).price(new BigDecimal("1000"))
				.build();
		Product product2 = Product.builder().id(productId2).name("MacBook").quantity(5).price(new BigDecimal("2000"))
				.build();

		Mockito.when(productService.getProductEntityById(productId1)).thenReturn(product1);
		Mockito.when(productService.getProductEntityById(productId2)).thenReturn(product2);

		BatchAvailabilityRequest request = BatchAvailabilityRequest.newBuilder()
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(productId1.toString())
						.setRequestedQuantity(3).build())
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(productId2.toString())
						.setRequestedQuantity(2).build())
				.build();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<BatchAvailabilityResponse> batchCaptor = ArgumentCaptor
				.forClass(BatchAvailabilityResponse.class);
		StreamObserver<BatchAvailabilityResponse> batchObserver = Mockito.mock(StreamObserver.class);

		// Act
		grpcService.batchCheckAvailability(request, batchObserver);

		// Assert
		Mockito.verify(batchObserver).onNext(batchCaptor.capture());
		Mockito.verify(batchObserver).onCompleted();

		BatchAvailabilityResponse batchResponse = batchCaptor.getValue();
		Assertions.assertThat(batchResponse.getResponsesList()).hasSize(2);

		ProductAvailabilityResponse response1 = batchResponse.getResponsesList().get(0);
		Assertions.assertThat(response1.getProductId()).isEqualTo(productId1.toString());
		Assertions.assertThat(response1.getIsAvailable()).isTrue();
		Assertions.assertThat(response1.getMessage()).isEqualTo("Available");

		ProductAvailabilityResponse response2 = batchResponse.getResponsesList().get(1);
		Assertions.assertThat(response2.getProductId()).isEqualTo(productId2.toString());
		Assertions.assertThat(response2.getIsAvailable()).isTrue();
		Assertions.assertThat(response2.getMessage()).isEqualTo("Available");
	}

	@Test
	void batchCheckAvailability_WhenSomeProductsNotFound_ReturnsMixedResults() {
		// Arrange
		UUID existingId = UUID.randomUUID();
		UUID missingId = UUID.randomUUID();

		Product existingProduct = Product.builder().id(existingId).name("Existing").quantity(10)
				.price(new BigDecimal("500")).build();

		Mockito.when(productService.getProductEntityById(existingId)).thenReturn(existingProduct);
		Mockito.when(productService.getProductEntityById(missingId)).thenThrow(new ProductNotFoundException(missingId));

		BatchAvailabilityRequest request = BatchAvailabilityRequest.newBuilder()
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(existingId.toString())
						.setRequestedQuantity(2).build())
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(missingId.toString())
						.setRequestedQuantity(1).build())
				.build();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<BatchAvailabilityResponse> batchCaptor = ArgumentCaptor
				.forClass(BatchAvailabilityResponse.class);
		StreamObserver<BatchAvailabilityResponse> batchObserver = Mockito.mock(StreamObserver.class);

		// Act
		grpcService.batchCheckAvailability(request, batchObserver);

		// Assert
		Mockito.verify(batchObserver).onNext(batchCaptor.capture());
		Mockito.verify(batchObserver).onCompleted();

		BatchAvailabilityResponse batchResponse = batchCaptor.getValue();
		Assertions.assertThat(batchResponse.getResponsesList()).hasSize(2);

		// Проверяем успешный ответ
		ProductAvailabilityResponse successResponse = batchResponse.getResponsesList().get(0);
		Assertions.assertThat(successResponse.getProductId()).isEqualTo(existingId.toString());
		Assertions.assertThat(successResponse.getIsAvailable()).isTrue();

		// Проверяем ответ с ошибкой
		ProductAvailabilityResponse errorResponse = batchResponse.getResponsesList().get(1);
		Assertions.assertThat(errorResponse.getProductId()).isEqualTo(missingId.toString());
		Assertions.assertThat(errorResponse.getIsAvailable()).isFalse();
		Assertions.assertThat(errorResponse.getMessage()).isEqualTo("Product not found");
		Assertions.assertThat(errorResponse.getAvailableQuantity()).isEqualTo(0);
	}

	@Test
	void batchCheckAvailability_WhenEmptyRequestList_ReturnsEmptyResponse() {
		// Arrange
		BatchAvailabilityRequest request = BatchAvailabilityRequest.newBuilder().build();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<BatchAvailabilityResponse> batchCaptor = ArgumentCaptor
				.forClass(BatchAvailabilityResponse.class);
		StreamObserver<BatchAvailabilityResponse> batchObserver = Mockito.mock(StreamObserver.class);

		// Act
		grpcService.batchCheckAvailability(request, batchObserver);

		// Assert
		Mockito.verify(batchObserver).onNext(batchCaptor.capture());
		Mockito.verify(batchObserver).onCompleted();

		BatchAvailabilityResponse batchResponse = batchCaptor.getValue();
		Assertions.assertThat(batchResponse.getResponsesList()).isEmpty();
	}

	@Test
	void batchCheckAvailability_WhenInvalidUUIDInBatch_ReturnsErrorForThatItemOnly() {
		// Arrange
		UUID validId = UUID.randomUUID();
		String invalidUuid = "invalid-uuid-format";

		Product validProduct = Product.builder().id(validId).name("Valid Product").quantity(5)
				.price(new BigDecimal("50")).build();

		Mockito.when(productService.getProductEntityById(validId)).thenReturn(validProduct);

		BatchAvailabilityRequest request = BatchAvailabilityRequest.newBuilder()
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(validId.toString())
						.setRequestedQuantity(2).build())
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(invalidUuid).setRequestedQuantity(1)
						.build())
				.build();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<BatchAvailabilityResponse> batchCaptor = ArgumentCaptor
				.forClass(BatchAvailabilityResponse.class);
		StreamObserver<BatchAvailabilityResponse> batchObserver = Mockito.mock(StreamObserver.class);

		// Act
		grpcService.batchCheckAvailability(request, batchObserver);

		// Assert
		Mockito.verify(batchObserver).onNext(batchCaptor.capture());
		Mockito.verify(batchObserver).onCompleted();

		BatchAvailabilityResponse batchResponse = batchCaptor.getValue();
		Assertions.assertThat(batchResponse.getResponsesList()).hasSize(2);

		// Первый - успешный
		ProductAvailabilityResponse successResponse = batchResponse.getResponsesList().get(0);
		Assertions.assertThat(successResponse.getProductId()).isEqualTo(validId.toString());
		Assertions.assertThat(successResponse.getIsAvailable()).isTrue();

		// Второй - с ошибкой
		ProductAvailabilityResponse errorResponse = batchResponse.getResponsesList().get(1);
		Assertions.assertThat(errorResponse.getProductId()).isEqualTo(invalidUuid);
		Assertions.assertThat(errorResponse.getIsAvailable()).isFalse();
		Assertions.assertThat(errorResponse.getMessage()).isEqualTo("Product not found");
	}

	@Test
	void batchCheckAvailability_WhenProductInsufficientQuantity_ReturnsNotAvailable() {
		// Arrange
		UUID productId1 = UUID.randomUUID();
		UUID productId2 = UUID.randomUUID();

		Product product1 = Product.builder().id(productId1).name("Product1").quantity(3).price(new BigDecimal("100"))
				.build();
		Product product2 = Product.builder().id(productId2).name("Product2").quantity(5).price(new BigDecimal("200"))
				.build();

		Mockito.when(productService.getProductEntityById(productId1)).thenReturn(product1);
		Mockito.when(productService.getProductEntityById(productId2)).thenReturn(product2);

		BatchAvailabilityRequest request = BatchAvailabilityRequest.newBuilder()
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(productId1.toString())
						.setRequestedQuantity(5) // больше чем есть (3)
						.build())
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(productId2.toString())
						.setRequestedQuantity(2) // достаточно
						.build())
				.build();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<BatchAvailabilityResponse> batchCaptor = ArgumentCaptor
				.forClass(BatchAvailabilityResponse.class);
		StreamObserver<BatchAvailabilityResponse> batchObserver = Mockito.mock(StreamObserver.class);

		// Act
		grpcService.batchCheckAvailability(request, batchObserver);

		// Assert
		Mockito.verify(batchObserver).onNext(batchCaptor.capture());
		Mockito.verify(batchObserver).onCompleted();

		BatchAvailabilityResponse batchResponse = batchCaptor.getValue();
		Assertions.assertThat(batchResponse.getResponsesList()).hasSize(2);

		// Первый - недостаточно
		ProductAvailabilityResponse response1 = batchResponse.getResponsesList().get(0);
		Assertions.assertThat(response1.getProductId()).isEqualTo(productId1.toString());
		Assertions.assertThat(response1.getIsAvailable()).isFalse();
		Assertions.assertThat(response1.getMessage()).isEqualTo("Insufficient stock");
		Assertions.assertThat(response1.getAvailableQuantity()).isEqualTo(3);

		// Второй - достаточно
		ProductAvailabilityResponse response2 = batchResponse.getResponsesList().get(1);
		Assertions.assertThat(response2.getProductId()).isEqualTo(productId2.toString());
		Assertions.assertThat(response2.getIsAvailable()).isTrue();
		Assertions.assertThat(response2.getMessage()).isEqualTo("Available");
		Assertions.assertThat(response2.getAvailableQuantity()).isEqualTo(5);
	}

	@Test
	void batchCheckAvailability_OrderOfResponsesMatchesOrderOfRequests() {
		// Arrange
		UUID[] productIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};

		for (UUID id : productIds) {
			Product product = Product.builder().id(id).name("Product").quantity(10).price(new BigDecimal("100"))
					.build();
			Mockito.when(productService.getProductEntityById(id)).thenReturn(product);
		}

		BatchAvailabilityRequest request = BatchAvailabilityRequest.newBuilder()
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(productIds[0].toString())
						.setRequestedQuantity(1).build())
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(productIds[1].toString())
						.setRequestedQuantity(2).build())
				.addRequests(ProductAvailabilityRequest.newBuilder().setProductId(productIds[2].toString())
						.setRequestedQuantity(3).build())
				.build();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<BatchAvailabilityResponse> batchCaptor = ArgumentCaptor
				.forClass(BatchAvailabilityResponse.class);
		StreamObserver<BatchAvailabilityResponse> batchObserver = Mockito.mock(StreamObserver.class);

		// Act
		grpcService.batchCheckAvailability(request, batchObserver);

		// Assert
		Mockito.verify(batchObserver).onNext(batchCaptor.capture());
		Mockito.verify(batchObserver).onCompleted();

		BatchAvailabilityResponse batchResponse = batchCaptor.getValue();

		// Проверяем порядок ответов
		for (int i = 0; i < productIds.length; i++) {
			Assertions.assertThat(batchResponse.getResponsesList().get(i).getProductId())
					.isEqualTo(productIds[i].toString());
		}
	}
}
