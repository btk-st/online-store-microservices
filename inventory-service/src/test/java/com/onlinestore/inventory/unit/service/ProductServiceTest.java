package com.onlinestore.inventory.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.repository.ProductRepository;
import com.onlinestore.inventory.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  @Mock private ProductMapper productMapper;

  @InjectMocks private ProductService productService;

  @Captor private ArgumentCaptor<Product> productCaptor;

  private UUID productId;
  private Product product;
  private ProductResponse productResponse;
  private CreateProductRequest createRequest;

  @BeforeEach
  void setUp() {
    productId = UUID.randomUUID();

    product =
        Product.builder()
            .id(productId)
            .name("MacBook Pro")
            .price(new BigDecimal("2499.99"))
            .quantity(10)
            .sale(new BigDecimal("5.00"))
            .build();

    productResponse =
        ProductResponse.builder()
            .id(productId)
            .name("MacBook Pro")
            .price(new BigDecimal("2499.99"))
            .quantity(10)
            .sale(new BigDecimal("5.00"))
            .build();

    createRequest =
        CreateProductRequest.builder()
            .name("MacBook Pro")
            .price(new BigDecimal("2499.99"))
            .quantity(10)
            .sale(new BigDecimal("5.00"))
            .build();
  }

  @Test
  void getAllProducts_shouldReturnListOfProductResponses() {

    Product product2 =
        Product.builder()
            .id(UUID.randomUUID())
            .name("iPhone")
            .price(new BigDecimal("999.99"))
            .quantity(20)
            .build();

    ProductResponse response2 =
        ProductResponse.builder()
            .id(product2.getId())
            .name("iPhone")
            .price(new BigDecimal("999.99"))
            .quantity(20)
            .build();

    when(productRepository.findAll()).thenReturn(List.of(product, product2));
    when(productMapper.toResponse(product)).thenReturn(productResponse);
    when(productMapper.toResponse(product2)).thenReturn(response2);

    List<ProductResponse> result = productService.getAllProducts();

    assertThat(result).hasSize(2).containsExactly(productResponse, response2);
    verify(productRepository).findAll();
    verify(productMapper, times(2)).toResponse(any(Product.class));
  }

  @Test
  void getAllProducts_shouldReturnEmptyList_whenNoProducts() {
    when(productRepository.findAll()).thenReturn(List.of());

    List<ProductResponse> result = productService.getAllProducts();

    assertThat(result).isEmpty();
    verify(productRepository).findAll();
    verifyNoInteractions(productMapper);
  }

  @Test
  void getProductById_shouldReturnProductResponse_whenProductExists() {

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productMapper.toResponse(product)).thenReturn(productResponse);

    ProductResponse result = productService.getProductById(productId);

    assertThat(result).isEqualTo(productResponse);
    verify(productRepository).findById(productId);
    verify(productMapper).toResponse(product);
  }

  @Test
  void getProductById_shouldThrowProductNotFoundException_whenProductNotFound() {

    when(productRepository.findById(productId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.getProductById(productId))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining(productId.toString());

    verify(productRepository).findById(productId);
    verify(productMapper, never()).toResponse(any());
  }

  @Test
  void createProduct_shouldSaveProductAndReturnResponse() {

    Product savedProduct =
        Product.builder()
            .id(productId)
            .name("MacBook Pro")
            .price(new BigDecimal("2499.99"))
            .quantity(10)
            .sale(new BigDecimal("5.00"))
            .build();

    when(productMapper.toEntity(createRequest)).thenReturn(product);
    when(productRepository.save(product)).thenReturn(savedProduct);
    when(productMapper.toResponse(savedProduct)).thenReturn(productResponse);

    ProductResponse result = productService.createProduct(createRequest);

    assertThat(result).isEqualTo(productResponse);
    verify(productMapper).toEntity(createRequest);
    verify(productRepository).save(product);
    verify(productMapper).toResponse(savedProduct);
  }

  @Test
  void updateProduct_shouldUpdateExistingProductAndReturnResponse() {

    CreateProductRequest updateRequest =
        CreateProductRequest.builder()
            .name("MacBook Pro M3")
            .price(new BigDecimal("2699.99"))
            .quantity(15)
            .sale(new BigDecimal("10.00"))
            .build();

    Product updatedProduct =
        Product.builder()
            .id(productId)
            .name("MacBook Pro M3")
            .price(new BigDecimal("2699.99"))
            .quantity(15)
            .sale(new BigDecimal("10.00"))
            .build();

    ProductResponse updatedResponse =
        ProductResponse.builder()
            .id(productId)
            .name("MacBook Pro M3")
            .price(new BigDecimal("2699.99"))
            .quantity(15)
            .sale(new BigDecimal("10.00"))
            .build();

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.save(product)).thenReturn(updatedProduct);
    when(productMapper.toResponse(updatedProduct)).thenReturn(updatedResponse);

    ProductResponse result = productService.updateProduct(productId, updateRequest);

    assertThat(result).isEqualTo(updatedResponse);
    verify(productRepository).findById(productId);
    verify(productRepository).save(product);
    verify(productMapper).toResponse(updatedProduct);

    assertThat(product.getName()).isEqualTo("MacBook Pro M3");
    assertThat(product.getPrice()).isEqualTo(new BigDecimal("2699.99"));
    assertThat(product.getQuantity()).isEqualTo(15);
    assertThat(product.getSale()).isEqualTo(new BigDecimal("10.00"));
  }

  @Test
  void updateProduct_shouldKeepSaleValue_whenSaleIsNullInRequest() {

    CreateProductRequest updateRequest =
        CreateProductRequest.builder()
            .name("MacBook Pro M3")
            .price(new BigDecimal("2699.99"))
            .quantity(15)
            .sale(null) // null в запросе
            .build();

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenReturn(product);
    when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

    productService.updateProduct(productId, updateRequest);

    verify(productRepository).save(productCaptor.capture());
    Product savedProduct = productCaptor.getValue();

    // Проверяем что sale остался прежним (5.00), а не стал null
    assertThat(savedProduct.getSale()).isEqualTo(new BigDecimal("5.00"));
  }

  @Test
  void updateProduct_shouldThrowProductNotFoundException_whenProductNotFound() {

    when(productRepository.findById(productId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> productService.updateProduct(productId, createRequest))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining(productId.toString());

    verify(productRepository).findById(productId);
    verify(productRepository, never()).save(any());
    verify(productMapper, never()).toResponse(any());
  }

  @Test
  void deleteProduct_shouldDeleteProduct_whenProductExists() {

    when(productRepository.existsById(productId)).thenReturn(true);

    productService.deleteProduct(productId);

    verify(productRepository).existsById(productId);
    verify(productRepository).deleteById(productId);
  }

  @Test
  void deleteProduct_shouldThrowProductNotFoundException_whenProductNotFound() {

    when(productRepository.existsById(productId)).thenReturn(false);

    assertThatThrownBy(() -> productService.deleteProduct(productId))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining(productId.toString());

    verify(productRepository).existsById(productId);
    verify(productRepository, never()).deleteById(productId);
  }

  @Test
  void findProductOrThrow_shouldReturnProduct_whenExists() {

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));

    productService.getProductById(productId);

    verify(productRepository).findById(productId);
  }

  @Test
  void findProductOrThrow_shouldThrow_whenNotFound() {

    when(productRepository.findById(productId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.getProductById(productId))
        .isInstanceOf(ProductNotFoundException.class);

    verify(productRepository).findById(productId);
  }
}
