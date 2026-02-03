package com.onlinestore.order.integration;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.order.dto.CreateOrderRequest;
import com.onlinestore.order.entity.Order;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.exception.ProductNotAvailableException;
import com.onlinestore.order.kafka.OrderCreatedEvent;
import com.onlinestore.order.repository.OrderRepository;
import com.onlinestore.order.repository.UserRepository;
import com.onlinestore.order.service.api.TransactionalOutboxService;

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
		testUser = userRepository.save(User.builder().username("orderuser").email("order@email.com")
				.password("encodedPass").role(User.Role.ROLE_USER).build());

		// Настраиваем успешный ответ от Inventory Service по умолчанию
		Mockito.when(inventoryClient.checkAvailabilityOrThrow(Mockito.any(UUID.class), Mockito.anyInt()))
				.thenReturn(com.onlinestore.order.grpc.ProductAvailabilityResponse.newBuilder()
						.setProductName("iPhone 15 Pro").setPrice(1299.99).setDiscount(15.50).setIsAvailable(true)
						.build());

		// Мокаем успешную отправку в Kafka
		Mockito.when(kafkaTemplate.send(Mockito.anyString(), Mockito.anyString(), Mockito.any(OrderCreatedEvent.class)))
				.thenReturn(null);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private void authenticateAs(User user) {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
	}

	@Test
	void createOrder_Success_ReturnsCreated() throws Exception {
		authenticateAs(testUser);

		UUID productId = UUID.randomUUID();
		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(),
				List.of(new CreateOrderRequest.OrderItemRequest(productId, 2)));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(testUser.getId().toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.items").isArray())
				.andExpect(MockMvcResultMatchers.jsonPath("$.items[0].productId").value(productId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.items[0].productName").value("iPhone 15 Pro"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.items[0].price").value(1299.99))
				.andExpect(MockMvcResultMatchers.jsonPath("$.items[0].sale").value(15.50));

		// Проверяем что заказ сохранен в БД
		List<Order> orders = orderRepository.findAll();
		Assertions.assertThat(orders).hasSize(1);
		Assertions.assertThat(orders.get(0).getUser().getId()).isEqualTo(testUser.getId());

		// Проверяем вызов gRPC клиента
		Mockito.verify(inventoryClient).checkAvailabilityOrThrow(Mockito.eq(productId), Mockito.eq(2));

		// Проверяем отправку в Kafka (через аутбокс)
		Mockito.verify(kafkaTemplate).send(Mockito.eq("orders"), Mockito.anyString(),
				Mockito.any(OrderCreatedEvent.class));

		// Проверяем что событие сохранено в аутбокс
		Mockito.verify(transactionalOutboxService).saveOrderCreatedEvent(orders.get(0));
	}

	@Test
	void createOrder_WhenUnauthenticated_ReturnsForbidden() throws Exception {
		CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(),
				List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 1)));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isForbidden());
	}

	@Test
	void createOrder_WhenUserIdMismatch_ReturnsBadRequest() throws Exception {
		authenticateAs(testUser);

		UUID differentUserId = UUID.randomUUID();
		CreateOrderRequest request = new CreateOrderRequest(differentUserId, // Не совпадает с аутентифицированным
																				// пользователем
				List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 1)));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("User ID mismatch")));
	}

	@Test
	void createOrder_WhenProductNotAvailable_ReturnsBadRequest() throws Exception {
		authenticateAs(testUser);

		UUID productId = UUID.randomUUID();
		// Мокаем что товар недоступен
		Mockito.when(inventoryClient.checkAvailabilityOrThrow(Mockito.any(UUID.class), Mockito.anyInt()))
				.thenThrow(new ProductNotAvailableException(productId, 1, 1, "test message"));

		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(),
				List.of(new CreateOrderRequest.OrderItemRequest(productId, 5)));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isUnprocessableEntity());

		// Проверяем что заказ НЕ сохранен в БД (транзакция откатилась)
		Assertions.assertThat(orderRepository.count()).isZero();
	}

	@Test
	void createOrder_WithMultipleItems_AllItemsChecked() throws Exception {
		authenticateAs(testUser);

		UUID product1 = UUID.randomUUID();
		UUID product2 = UUID.randomUUID();
		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(),
				List.of(new CreateOrderRequest.OrderItemRequest(product1, 1),
						new CreateOrderRequest.OrderItemRequest(product2, 3)));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isCreated());

		// Проверяем что оба товара были проверены через gRPC
		Mockito.verify(inventoryClient).checkAvailabilityOrThrow(Mockito.eq(product1), Mockito.eq(1));
		Mockito.verify(inventoryClient).checkAvailabilityOrThrow(Mockito.eq(product2), Mockito.eq(3));
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

		mockMvc.perform(
				MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	void createOrder_WithEmptyItems_ReturnsBadRequest() throws Exception {
		authenticateAs(testUser);

		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(), List.of() // Пустой список товаров
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isBadRequest());
	}
}
