package com.onlinestore.inventory.unit.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.repository.ProductRepository;
import com.onlinestore.inventory.service.ProductServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductMapper productMapper;

	@InjectMocks
	private ProductServiceImpl productService;

	@Captor
	private ArgumentCaptor<Product> productCaptor;

	private UUID productId;
	private Product product;
	private ProductResponse productResponse;
	private CreateProductRequest createRequest;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();

		product = Product.builder().id(productId).name("MacBook Pro").price(new BigDecimal("2499.99")).quantity(10)
				.sale(new BigDecimal("5.00")).build();

		productResponse = ProductResponse.builder().id(productId).name("MacBook Pro").price(new BigDecimal("2499.99"))
				.quantity(10).sale(new BigDecimal("5.00")).build();

		createRequest = CreateProductRequest.builder().name("MacBook Pro").price(new BigDecimal("2499.99")).quantity(10)
				.sale(new BigDecimal("5.00")).build();
	}

	@Test
	void getAllProducts_shouldReturnListOfProductResponses() {

		Product product2 = Product.builder().id(UUID.randomUUID()).name("iPhone").price(new BigDecimal("999.99"))
				.quantity(20).build();

		ProductResponse response2 = ProductResponse.builder().id(product2.getId()).name("iPhone")
				.price(new BigDecimal("999.99")).quantity(20).build();

		Mockito.when(productRepository.findAll()).thenReturn(List.of(product, product2));
		Mockito.when(productMapper.toResponse(product)).thenReturn(productResponse);
		Mockito.when(productMapper.toResponse(product2)).thenReturn(response2);

		List<ProductResponse> result = productService.getAllProducts();

		Assertions.assertThat(result).hasSize(2).containsExactly(productResponse, response2);
		Mockito.verify(productRepository).findAll();
		Mockito.verify(productMapper, Mockito.times(2)).toResponse(Mockito.any(Product.class));
	}

	@Test
	void getAllProducts_shouldReturnEmptyList_whenNoProducts() {
		Mockito.when(productRepository.findAll()).thenReturn(List.of());

		List<ProductResponse> result = productService.getAllProducts();

		Assertions.assertThat(result).isEmpty();
		Mockito.verify(productRepository).findAll();
		Mockito.verifyNoInteractions(productMapper);
	}

	@Test
	void getProductById_shouldReturnProductResponse_whenProductExists() {

		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		Mockito.when(productMapper.toResponse(product)).thenReturn(productResponse);

		ProductResponse result = productService.getProductById(productId);

		Assertions.assertThat(result).isEqualTo(productResponse);
		Mockito.verify(productRepository).findById(productId);
		Mockito.verify(productMapper).toResponse(product);
	}

	@Test
	void getProductById_shouldThrowProductNotFoundException_whenProductNotFound() {

		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.empty());

		Assertions.assertThatThrownBy(() -> productService.getProductById(productId))
				.isInstanceOf(ProductNotFoundException.class).hasMessageContaining(productId.toString());

		Mockito.verify(productRepository).findById(productId);
		Mockito.verify(productMapper, Mockito.never()).toResponse(Mockito.any());
	}

	@Test
	void createProduct_shouldSaveProductAndReturnResponse() {

		Product savedProduct = Product.builder().id(productId).name("MacBook Pro").price(new BigDecimal("2499.99"))
				.quantity(10).sale(new BigDecimal("5.00")).build();

		Mockito.when(productMapper.toEntity(createRequest)).thenReturn(product);
		Mockito.when(productRepository.save(product)).thenReturn(savedProduct);
		Mockito.when(productMapper.toResponse(savedProduct)).thenReturn(productResponse);

		ProductResponse result = productService.createProduct(createRequest);

		Assertions.assertThat(result).isEqualTo(productResponse);
		Mockito.verify(productMapper).toEntity(createRequest);
		Mockito.verify(productRepository).save(product);
		Mockito.verify(productMapper).toResponse(savedProduct);
	}

	@Test
	void deleteProduct_shouldDeleteProduct_whenProductExists() {

		Mockito.when(productRepository.existsById(productId)).thenReturn(true);

		productService.deleteProduct(productId);

		Mockito.verify(productRepository).existsById(productId);
		Mockito.verify(productRepository).deleteById(productId);
	}

	@Test
	void deleteProduct_shouldThrowProductNotFoundException_whenProductNotFound() {

		Mockito.when(productRepository.existsById(productId)).thenReturn(false);

		Assertions.assertThatThrownBy(() -> productService.deleteProduct(productId))
				.isInstanceOf(ProductNotFoundException.class).hasMessageContaining(productId.toString());

		Mockito.verify(productRepository).existsById(productId);
		Mockito.verify(productRepository, Mockito.never()).deleteById(productId);
	}

	@Test
	void findProductOrThrow_shouldReturnProduct_whenExists() {

		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		productService.getProductById(productId);

		Mockito.verify(productRepository).findById(productId);
	}

	@Test
	void findProductOrThrow_shouldThrow_whenNotFound() {

		Mockito.when(productRepository.findById(productId)).thenReturn(Optional.empty());

		Assertions.assertThatThrownBy(() -> productService.getProductById(productId))
				.isInstanceOf(ProductNotFoundException.class);

		Mockito.verify(productRepository).findById(productId);
	}
}
