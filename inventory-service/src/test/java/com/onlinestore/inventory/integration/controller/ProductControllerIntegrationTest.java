package com.onlinestore.inventory.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ProductRepository productRepository;

  @Autowired private ObjectMapper objectMapper;

  private UUID existingProductId;

  @BeforeEach
  void setUp() {
    // Очищаем БД перед каждым тестом
    productRepository.deleteAll();

    // Создаем тестовый продукт
    Product existingProduct =
        Product.builder()
            .name("MacBook Pro 16")
            .price(new BigDecimal("2499.99"))
            .quantity(10)
            .sale(new BigDecimal("5.00"))
            .build();

    existingProduct = productRepository.save(existingProduct);
    existingProductId = existingProduct.getId();
  }

  @Test
  void getAllProducts_shouldReturnAllProducts() throws Exception {

    Product secondProduct =
        Product.builder().name("iPhone 15").price(new BigDecimal("999.99")).quantity(20).build();
    productRepository.save(secondProduct);

    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name", is("MacBook Pro 16")))
        .andExpect(jsonPath("$[0].price", is(2499.99)))
        .andExpect(jsonPath("$[1].name", is("iPhone 15")))
        .andExpect(jsonPath("$[1].price", is(999.99)));
  }

  @Test
  void getAllProducts_shouldReturnEmptyList_whenNoProducts() throws Exception {

    productRepository.deleteAll();

    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void getProductById_shouldReturnProduct_whenProductExists() throws Exception {

    mockMvc
        .perform(get("/api/products/{id}", existingProductId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(existingProductId.toString())))
        .andExpect(jsonPath("$.name", is("MacBook Pro 16")))
        .andExpect(jsonPath("$.price", is(2499.99)))
        .andExpect(jsonPath("$.quantity", is(10)))
        .andExpect(jsonPath("$.sale", is(5.00)));
  }

  @Test
  void getProductById_shouldReturn404_whenProductNotFound() throws Exception {

    UUID nonExistentId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/products/{id}", nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.error", is("Not Found")))
        .andExpect(jsonPath("$.message", containsString(nonExistentId.toString())))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void getProductById_shouldReturn400_whenInvalidUUID() throws Exception {

    mockMvc
        .perform(get("/api/products/{id}", "invalid-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.error", is("Bad Request")));
  }

  @Test
  void createProduct_shouldReturn201AndCreatedProduct() throws Exception {

    CreateProductRequest request =
        CreateProductRequest.builder()
            .name("iPad Air")
            .price(new BigDecimal("599.99"))
            .quantity(15)
            .sale(new BigDecimal("10.00"))
            .build();

    String requestBody = objectMapper.writeValueAsString(request);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name", is("iPad Air")))
        .andExpect(jsonPath("$.price", is(599.99)))
        .andExpect(jsonPath("$.quantity", is(15)))
        .andExpect(jsonPath("$.sale", is(10.00)));

    assertThat(productRepository.count()).isEqualTo(2);
  }

  @Test
  void createProduct_shouldReturn400_whenNameIsBlank() throws Exception {

    CreateProductRequest request =
        CreateProductRequest.builder()
            .name("") // Blank name
            .price(new BigDecimal("100.00"))
            .quantity(10)
            .build();

    String requestBody = objectMapper.writeValueAsString(request);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.errors.name", notNullValue()));
  }

  @Test
  void createProduct_shouldReturn400_whenPriceIsNull() throws Exception {

    CreateProductRequest request =
        CreateProductRequest.builder()
            .name("Test Product")
            .price(null) // null price
            .quantity(10)
            .build();

    String requestBody = objectMapper.writeValueAsString(request);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.errors.price", notNullValue()));
  }

  @Test
  void createProduct_shouldReturn400_whenPriceIsNegative() throws Exception {

    CreateProductRequest request =
        CreateProductRequest.builder()
            .name("Test Product")
            .price(new BigDecimal("-100.00")) // negative price
            .quantity(10)
            .build();

    String requestBody = objectMapper.writeValueAsString(request);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.errors.price", notNullValue()));
  }

  @Test
  void createProduct_shouldReturn400_whenQuantityIsNegative() throws Exception {

    CreateProductRequest request =
        CreateProductRequest.builder()
            .name("Test Product")
            .price(new BigDecimal("100.00"))
            .quantity(-5) // negative quantity
            .build();

    String requestBody = objectMapper.writeValueAsString(request);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.errors.quantity", notNullValue()));
  }

  @Test
  void deleteProduct_shouldDeleteProductAndReturn204() throws Exception {

    mockMvc
        .perform(delete("/api/products/{id}", existingProductId))
        .andExpect(status().isNoContent());

    assertThat(productRepository.existsById(existingProductId)).isFalse();
    assertThat(productRepository.count()).isZero();
  }

  @Test
  void deleteProduct_shouldReturn404_whenProductNotFound() throws Exception {

    UUID nonExistentId = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/products/{id}", nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.error", is("Not Found")));
  }

  @Test
  void deleteProduct_shouldReturn400_whenInvalidUUID() throws Exception {

    mockMvc
        .perform(delete("/api/products/{id}", "invalid-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.error", is("Bad Request")));
  }

  @Test
  void shouldReturnProperErrorResponseStructure() throws Exception {
    // Проверяем структуру всех error response
    mockMvc
        .perform(get("/api/products/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.timestamp").isString())
        .andExpect(jsonPath("$.status").isNumber())
        .andExpect(jsonPath("$.error").isString())
        .andExpect(jsonPath("$.message").isString());
  }
}
