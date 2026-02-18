package com.onlinestore.inventory.integration.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private UUID existingProductId;

	@BeforeEach
	void setUp() {
		// Очищаем БД перед каждым тестом
		productRepository.deleteAll();

		// Создаем тестовый продукт
		Product existingProduct = Product.builder().name("MacBook Pro 16").price(new BigDecimal("2499.99")).quantity(10)
				.sale(new BigDecimal("5.00")).build();

		existingProduct = productRepository.save(existingProduct);
		existingProductId = existingProduct.getId();
	}

	@Test
	void getAllProducts_shouldReturnAllProducts() throws Exception {

		Product secondProduct = Product.builder().name("iPhone 15").price(new BigDecimal("999.99")).quantity(20)
				.build();
		productRepository.save(secondProduct);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/products")).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].name", Matchers.is("MacBook Pro 16")))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].price", Matchers.is(2499.99)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[1].name", Matchers.is("iPhone 15")))
				.andExpect(MockMvcResultMatchers.jsonPath("$[1].price", Matchers.is(999.99)));
	}

	@Test
	void getAllProducts_shouldReturnEmptyList_whenNoProducts() throws Exception {

		productRepository.deleteAll();

		mockMvc.perform(MockMvcRequestBuilders.get("/api/products")).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(0)));
	}

	@Test
	void getProductById_shouldReturnProduct_whenProductExists() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/api/products/{id}", existingProductId))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(existingProductId.toString())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("MacBook Pro 16")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.price", Matchers.is(2499.99)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.quantity", Matchers.is(10)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.sale", Matchers.is(5.00)));
	}

	@Test
	void getProductById_shouldReturn404_whenProductNotFound() throws Exception {

		UUID nonExistentId = UUID.randomUUID();

		mockMvc.perform(MockMvcRequestBuilders.get("/api/products/{id}", nonExistentId))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(404)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.error", Matchers.is("Not Found")))
				.andExpect(
						MockMvcResultMatchers.jsonPath("$.message", Matchers.containsString(nonExistentId.toString())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists());
	}

	@Test
	void getProductById_shouldReturn400_whenInvalidUUID() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/api/products/{id}", "invalid-uuid"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(400)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.error", Matchers.is("Bad Request")));
	}

	@Test
	void createProduct_shouldReturn201AndCreatedProduct() throws Exception {

		CreateProductRequest request = CreateProductRequest.builder().name("iPad Air").price(new BigDecimal("599.99"))
				.quantity(15).sale(new BigDecimal("10.00")).build();

		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/products").contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)).andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
				.andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("iPad Air")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.price", Matchers.is(599.99)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.quantity", Matchers.is(15)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.sale", Matchers.is(10.00)));

		Assertions.assertThat(productRepository.count()).isEqualTo(2);
	}

	@Test
	void createProduct_shouldReturn400_whenNameIsBlank() throws Exception {

		CreateProductRequest request = CreateProductRequest.builder().name("") // Blank name
				.price(new BigDecimal("100.00")).quantity(10).build();

		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/products").contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)).andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(400)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errors.name", Matchers.notNullValue()));
	}

	@Test
	void createProduct_shouldReturn400_whenPriceIsNull() throws Exception {

		CreateProductRequest request = CreateProductRequest.builder().name("Test Product").price(null) // null price
				.quantity(10).build();

		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/products").contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)).andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(400)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errors.price", Matchers.notNullValue()));
	}

	@Test
	void createProduct_shouldReturn400_whenPriceIsNegative() throws Exception {

		CreateProductRequest request = CreateProductRequest.builder().name("Test Product")
				.price(new BigDecimal("-100.00")) // negative price
				.quantity(10).build();

		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/products").contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)).andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(400)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errors.price", Matchers.notNullValue()));
	}

	@Test
	void createProduct_shouldReturn400_whenQuantityIsNegative() throws Exception {

		CreateProductRequest request = CreateProductRequest.builder().name("Test Product")
				.price(new BigDecimal("100.00")).quantity(-5) // negative quantity
				.build();

		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/products").contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)).andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(400)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errors.quantity", Matchers.notNullValue()));
	}

	@Test
	void deleteProduct_shouldDeleteProductAndReturn204() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/products/{id}", existingProductId))
				.andExpect(MockMvcResultMatchers.status().isNoContent());

		Assertions.assertThat(productRepository.existsById(existingProductId)).isFalse();
		Assertions.assertThat(productRepository.count()).isZero();
	}

	@Test
	void deleteProduct_shouldReturn404_whenProductNotFound() throws Exception {

		UUID nonExistentId = UUID.randomUUID();

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/products/{id}", nonExistentId))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(404)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.error", Matchers.is("Not Found")));
	}

	@Test
	void deleteProduct_shouldReturn400_whenInvalidUUID() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/products/{id}", "invalid-uuid"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(400)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.error", Matchers.is("Bad Request")));
	}

	@Test
	void shouldReturnProperErrorResponseStructure() throws Exception {
		// Проверяем структуру всех error response
		mockMvc.perform(MockMvcRequestBuilders.get("/api/products/{id}", UUID.randomUUID()))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").isString())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").isNumber())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").isString())
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").isString());
	}
}
