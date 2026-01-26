package com.onlinestore.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.exception.ProductNotAvailableException;
import com.onlinestore.order.kafka.OrderCreatedEvent;
import com.onlinestore.order.repository.OrderRepository;
import com.onlinestore.order.repository.UserRepository;
import com.onlinestore.order.service.TransactionalOutboxService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private TransactionalOutboxService transactionalOutboxService;

    @MockBean
    private com.onlinestore.order.grpc.InventoryGrpcClient inventoryClient;

    @MockBean
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    private User testUser;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = userRepository.save(
                User.builder()
                        .username("orderuser")
                        .email("order@email.com")
                        .password("encodedPass")
                        .role(User.Role.ROLE_USER)
                        .build()
        );

        // Настраиваем успешный ответ от Inventory Service по умолчанию
        when(inventoryClient.checkAvailabilityOrThrow(any(UUID.class), anyInt()))
                .thenReturn(
                        com.onlinestore.order.grpc.ProductAvailabilityResponse.newBuilder()
                                .setProductName("iPhone 15 Pro")
                                .setPrice(1299.99)
                                .setDiscount(15.50)
                                .setIsAvailable(true)
                                .build()
                );

        // Мокаем успешную отправку в Kafka
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderCreatedEvent.class)))
                .thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @Test
    void createOrder_Success_ReturnsCreated() throws Exception {
        authenticateAs(testUser);

        UUID productId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                testUser.getId(),
                List.of(new CreateOrderRequest.OrderItemRequest(productId, 2))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.items[0].price").value(1299.99))
                .andExpect(jsonPath("$.items[0].sale").value(15.50));

        // Проверяем что заказ сохранен в БД
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUser().getId()).isEqualTo(testUser.getId());

        // Проверяем вызов gRPC клиента
        verify(inventoryClient).checkAvailabilityOrThrow(eq(productId), eq(2));

        // Проверяем отправку в Kafka (через аутбокс)
        verify(kafkaTemplate).send(eq("orders"), anyString(), any(OrderCreatedEvent.class));

        //Проверяем что событие сохранено в аутбокс
        verify(transactionalOutboxService).saveOrderCreatedEvent(orders.get(0));
    }

    @Test
    void createOrder_WhenUnauthenticated_ReturnsForbidden() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 1))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_WhenUserIdMismatch_ReturnsBadRequest() throws Exception {
        authenticateAs(testUser);

        UUID differentUserId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                differentUserId, // Не совпадает с аутентифицированным пользователем
                List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 1))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("User ID mismatch")));
    }

    @Test
    void createOrder_WhenProductNotAvailable_ReturnsBadRequest() throws Exception {
        authenticateAs(testUser);

        UUID productId = UUID.randomUUID();
        // Мокаем что товар недоступен
        when(inventoryClient.checkAvailabilityOrThrow(any(UUID.class), anyInt()))
                .thenThrow(new ProductNotAvailableException(
                        productId,
                        1,
                        1,
                        "test message"));

        CreateOrderRequest request = new CreateOrderRequest(
                testUser.getId(),
                List.of(new CreateOrderRequest.OrderItemRequest(productId, 5))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        // Проверяем что заказ НЕ сохранен в БД (транзакция откатилась)
        assertThat(orderRepository.count()).isZero();
    }


    @Test
    void createOrder_WithMultipleItems_AllItemsChecked() throws Exception {
        authenticateAs(testUser);

        UUID product1 = UUID.randomUUID();
        UUID product2 = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                testUser.getId(),
                List.of(
                        new CreateOrderRequest.OrderItemRequest(product1, 1),
                        new CreateOrderRequest.OrderItemRequest(product2, 3)
                )
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Проверяем что оба товара были проверены через gRPC
        verify(inventoryClient).checkAvailabilityOrThrow(eq(product1), eq(1));
        verify(inventoryClient).checkAvailabilityOrThrow(eq(product2), eq(3));
    }

    @Test
    void createOrder_InvalidRequest_ReturnsBadRequest() throws Exception {
        authenticateAs(testUser);

        // Невалидный запрос: отрицательное количество
        String invalidJson = """
            {
                "userId": "%s",
                "items": [
                    {
                        "productId": "%s",
                        "quantity": -1
                    }
                ]
            }
            """.formatted(testUser.getId(), UUID.randomUUID());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_WithEmptyItems_ReturnsBadRequest() throws Exception {
        authenticateAs(testUser);

        CreateOrderRequest request = new CreateOrderRequest(
                testUser.getId(),
                List.of() // Пустой список товаров
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
