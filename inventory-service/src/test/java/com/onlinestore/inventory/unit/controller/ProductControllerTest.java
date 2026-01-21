package com.onlinestore.inventory.unit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.inventory.controller.ProductController;
import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.exception.GlobalExceptionHandler;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private ProductService productService;

  @InjectMocks private ProductController productController;

  private UUID productId;
  private ProductResponse productResponse;
  private CreateProductRequest createRequest;

  @BeforeEach
  void setUp() {
    // Настраиваем MockMvc без Spring контекста
    mockMvc =
        MockMvcBuilders.standaloneSetup(productController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    productId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

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
  void getAllProducts_shouldReturn200AndProductList() throws Exception {

    ProductResponse response2 =
        ProductResponse.builder()
            .id(UUID.randomUUID())
            .name("iPhone")
            .price(new BigDecimal("999.99"))
            .quantity(20)
            .build();

    when(productService.getAllProducts()).thenReturn(List.of(productResponse, response2));

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/api/products").contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);

    verify(productService).getAllProducts();
  }

  @Test
  void getProductById_shouldReturn200AndProduct_whenProductExists() throws Exception {

    when(productService.getProductById(productId)).thenReturn(productResponse);

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/api/products/{id}", productId).contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    verify(productService).getProductById(productId);
  }

  @Test
  void getProductById_shouldPropagateNotFoundException() throws Exception {

    when(productService.getProductById(productId))
        .thenThrow(new ProductNotFoundException(productId));

    MockHttpServletResponse response =
        mockMvc.perform(get("/api/products/{id}", productId)).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    verify(productService).getProductById(productId);
  }

  @Test
  void getProductById_shouldReturn400_whenInvalidUUID() throws Exception {

    MockHttpServletResponse response =
        mockMvc.perform(get("/api/products/{id}", "invalid-uuid")).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    verify(productService, never()).getProductById(any());
  }

  @Test
  void createProduct_shouldReturn201AndCreatedProduct() throws Exception {
    when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(productResponse);

    String requestBody = objectMapper.writeValueAsString(createRequest);

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/api/products").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
    verify(productService).createProduct(any(CreateProductRequest.class));
  }

  @Test
  void updateProduct_shouldReturn200AndUpdatedProduct() throws Exception {

    when(productService.updateProduct(eq(productId), any(CreateProductRequest.class)))
        .thenReturn(productResponse);

    String requestBody = objectMapper.writeValueAsString(createRequest);

    MockHttpServletResponse response =
        mockMvc
            .perform(
                put("/api/products/{id}", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    verify(productService).updateProduct(eq(productId), any(CreateProductRequest.class));
  }

  @Test
  void updateProduct_shouldReturn404_whenProductNotFound() throws Exception {

    when(productService.updateProduct(eq(productId), any(CreateProductRequest.class)))
        .thenThrow(new ProductNotFoundException(productId));

    String requestBody = objectMapper.writeValueAsString(createRequest);

    MockHttpServletResponse response =
        mockMvc
            .perform(
                put("/api/products/{id}", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void deleteProduct_shouldReturn204() throws Exception {

    doNothing().when(productService).deleteProduct(productId);

    MockHttpServletResponse response =
        mockMvc.perform(delete("/api/products/{id}", productId)).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
    verify(productService).deleteProduct(productId);
  }

  @Test
  void deleteProduct_shouldReturn404_whenProductNotFound() throws Exception {

    doThrow(new ProductNotFoundException(productId)).when(productService).deleteProduct(productId);

    MockHttpServletResponse response =
        mockMvc.perform(delete("/api/products/{id}", productId)).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    verify(productService).deleteProduct(productId);
  }

  @Test
  void deleteProduct_shouldReturn400_whenInvalidUUID() throws Exception {

    MockHttpServletResponse response =
        mockMvc.perform(delete("/api/products/{id}", "invalid-uuid")).andReturn().getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    verify(productService, never()).deleteProduct(any());
  }
}
