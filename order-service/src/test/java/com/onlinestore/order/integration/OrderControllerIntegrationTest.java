package com.onlinestore.order.integration;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.onlinestore.order.grpc.BatchAvailabilityResponse;
import com.onlinestore.order.grpc.ProductAvailabilityResponse;
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

		// Настраиваем УСПЕШНЫЙ batch ответ от Inventory Service по умолчанию
		BatchAvailabilityResponse successBatchResponse = BatchAvailabilityResponse.newBuilder()
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(UUID.randomUUID().toString())
						.setProductName("iPhone 15 Pro").setPrice(1299.99).setDiscount(15.50).setIsAvailable(true)
						.setAvailableQuantity(10).build())
				.build();

		Mockito.when(inventoryClient.batchCheckAvailability(Mockito.any(CreateOrderRequest.class)))
				.thenReturn(successBatchResponse);

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

		// Настраиваем batch ответ для конкретного productId
		BatchAvailabilityResponse batchResponse = BatchAvailabilityResponse.newBuilder()
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(productId.toString())
						.setProductName("iPhone 15 Pro").setPrice(1299.99).setDiscount(15.50).setIsAvailable(true)
						.setAvailableQuantity(10).build())
				.build();

		Mockito.when(inventoryClient.batchCheckAvailability(Mockito.any(CreateOrderRequest.class)))
				.thenReturn(batchResponse);

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

		// Проверяем вызов gRPC клиента ТОЛЬКО ОДИН РАЗ (batch)
		Mockito.verify(inventoryClient, Mockito.times(1)).batchCheckAvailability(Mockito.any(CreateOrderRequest.class));

		// Проверяем отправку в Kafka
		Mockito.verify(kafkaTemplate).send(Mockito.eq("orders"), Mockito.anyString(),
				Mockito.any(OrderCreatedEvent.class));

		// Проверяем что событие сохранено в аутбокс
		Mockito.verify(transactionalOutboxService).saveOrderCreatedEvent(orders.get(0));
	}

	@Test
	void createOrder_WithMultipleItems_AllItemsCheckedInBatch() throws Exception {
		authenticateAs(testUser);

		UUID product1 = UUID.randomUUID();
		UUID product2 = UUID.randomUUID();

		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(),
				List.of(new CreateOrderRequest.OrderItemRequest(product1, 1),
						new CreateOrderRequest.OrderItemRequest(product2, 3)));

		// Настраиваем batch ответ с двумя товарами
		BatchAvailabilityResponse batchResponse = BatchAvailabilityResponse.newBuilder()
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(product1.toString())
						.setProductName("iPhone 15 Pro").setPrice(1299.99).setIsAvailable(true).setAvailableQuantity(10)
						.build())
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(product2.toString())
						.setProductName("MacBook Pro").setPrice(2499.99).setIsAvailable(true).setAvailableQuantity(5)
						.build())
				.build();

		Mockito.when(inventoryClient.batchCheckAvailability(Mockito.any(CreateOrderRequest.class)))
				.thenReturn(batchResponse);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isCreated());

		// Проверяем что был ОДИН batch вызов
		Mockito.verify(inventoryClient, Mockito.times(1)).batchCheckAvailability(Mockito.any(CreateOrderRequest.class));
	}

	@Test
	void createOrder_WhenProductNotAvailableInBatch_ReturnsUnprocessableEntity() throws Exception {
		authenticateAs(testUser);

		UUID productId = UUID.randomUUID();

		// Настраиваем batch ответ где товар НЕ доступен
		BatchAvailabilityResponse batchResponse = BatchAvailabilityResponse.newBuilder()
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(productId.toString())
						.setProductName("iPhone 15 Pro").setPrice(1299.99).setIsAvailable(false) // ← НЕ доступен!
						.setAvailableQuantity(1).setMessage("Insufficient stock").build())
				.build();

		Mockito.when(inventoryClient.batchCheckAvailability(Mockito.any(CreateOrderRequest.class)))
				.thenReturn(batchResponse);

		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(),
				List.of(new CreateOrderRequest.OrderItemRequest(productId, 5)));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isUnprocessableEntity());

		// Проверяем что заказ НЕ сохранен в БД
		Assertions.assertThat(orderRepository.count()).isZero();
	}

	@Test
	void createOrder_WhenMixedAvailabilityInBatch_ReturnsUnprocessableEntity() throws Exception {
		authenticateAs(testUser);

		UUID availableProduct = UUID.randomUUID();
		UUID unavailableProduct = UUID.randomUUID();

		// Batch ответ: один товар доступен, второй нет
		BatchAvailabilityResponse batchResponse = BatchAvailabilityResponse.newBuilder()
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(availableProduct.toString())
						.setProductName("Available Product").setPrice(100.0).setIsAvailable(true)
						.setAvailableQuantity(10).build())
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(unavailableProduct.toString())
						.setProductName("Unavailable Product").setPrice(200.0).setIsAvailable(false) // ← не доступен
						.setAvailableQuantity(2).setMessage("Only 2 items in stock").build())
				.build();

		Mockito.when(inventoryClient.batchCheckAvailability(Mockito.any(CreateOrderRequest.class)))
				.thenReturn(batchResponse);

		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(),
				List.of(new CreateOrderRequest.OrderItemRequest(availableProduct, 2),
						new CreateOrderRequest.OrderItemRequest(unavailableProduct, 5) // запрашиваем 5, есть только 2
				));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isUnprocessableEntity());

		// Проверяем что заказ НЕ сохранен (вся транзакция откатывается)
		Assertions.assertThat(orderRepository.count()).isZero();
	}

	@Test
	void createOrder_OrderOfResponsesMatchesOrderOfRequests() throws Exception {
		authenticateAs(testUser);

		UUID product1 = UUID.randomUUID();
		UUID product2 = UUID.randomUUID();
		UUID product3 = UUID.randomUUID();

		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(),
				List.of(new CreateOrderRequest.OrderItemRequest(product1, 1),
						new CreateOrderRequest.OrderItemRequest(product2, 2),
						new CreateOrderRequest.OrderItemRequest(product3, 3)));

		// Важно: порядок ответов должен соответствовать порядку запросов
		BatchAvailabilityResponse batchResponse = BatchAvailabilityResponse.newBuilder()
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(product1.toString())
						.setProductName("Product 1").setPrice(100.0).setIsAvailable(true).setAvailableQuantity(10)
						.build())
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(product2.toString())
						.setProductName("Product 2").setPrice(200.0).setIsAvailable(true).setAvailableQuantity(5)
						.build())
				.addResponses(ProductAvailabilityResponse.newBuilder().setProductId(product3.toString())
						.setProductName("Product 3").setPrice(300.0).setIsAvailable(true).setAvailableQuantity(3)
						.build())
				.build();

		Mockito.when(inventoryClient.batchCheckAvailability(Mockito.any(CreateOrderRequest.class)))
				.thenReturn(batchResponse);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isCreated());

		// Проверяем что inventoryClient получает правильный CreateOrderRequest
		ArgumentCaptor<CreateOrderRequest> requestCaptor = ArgumentCaptor.forClass(CreateOrderRequest.class);

		Mockito.verify(inventoryClient).batchCheckAvailability(requestCaptor.capture());

		CreateOrderRequest sentRequest = requestCaptor.getValue();

		// Проверяем что порядок товаров в запросе сохранился
		Assertions.assertThat(sentRequest.getItems()).extracting(CreateOrderRequest.OrderItemRequest::getProductId)
				.containsExactly(product1, product2, product3);

		// Проверяем количество
		Assertions.assertThat(sentRequest.getItems()).extracting(CreateOrderRequest.OrderItemRequest::getQuantity)
				.containsExactly(1, 2, 3);
	}

	@Test
	void createOrder_WithEmptyItems_ReturnsBadRequest() throws Exception {
		authenticateAs(testUser);

		CreateOrderRequest request = new CreateOrderRequest(testUser.getId(), List.of());

		mockMvc.perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))).andExpect(MockMvcResultMatchers.status().isBadRequest());

		// gRPC не должен вызываться при пустом списке
		Mockito.verify(inventoryClient, Mockito.never()).batchCheckAvailability(Mockito.any());
	}
}
