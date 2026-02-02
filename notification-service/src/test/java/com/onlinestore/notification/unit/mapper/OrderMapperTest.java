package com.onlinestore.notification.unit.mapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlinestore.notification.dto.OrderDto;
import com.onlinestore.notification.dto.OrderDto.OrderItemDto;
import com.onlinestore.notification.entity.OrderEntity;
import com.onlinestore.notification.mapper.OrderMapper;

@ExtendWith(MockitoExtension.class)
class OrderMapperTest {

	@InjectMocks
	private OrderMapper orderMapper;

	@Test
	void toOrderDto_shouldReturnNull_whenInputIsNull() {
		// When
		OrderDto result = orderMapper.toOrderDto(null);

		// Then
		Assertions.assertThat(result).isNull();
	}

	@Test
	void toOrderDto_shouldReturnNull_whenInputIsEmpty() {
		// When
		OrderDto result = orderMapper.toOrderDto(Collections.emptyList());

		// Then
		Assertions.assertThat(result).isNull();
	}

	@Test
	void toOrderDto_shouldMapSingleEntityCorrectly() {
		// Given
		UUID orderId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();

		OrderEntity entity = OrderEntity.builder().orderId(orderId).userId(userId).productId(productId).quantity(2)
				.price(new BigDecimal("100.00")).sale(new BigDecimal("10.00")).totalPrice(new BigDecimal("180.00")) // (100-10)*2
																													// =
																													// 180
				.build();

		List<OrderEntity> entities = List.of(entity);

		// When
		OrderDto result = orderMapper.toOrderDto(entities);

		// Then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result.getOrderId()).isEqualTo(orderId);
		Assertions.assertThat(result.getUserId()).isEqualTo(userId);
		Assertions.assertThat(result.getItems()).hasSize(1);
		Assertions.assertThat(result.getTotalPrice()).isEqualByComparingTo("180.00");

		OrderItemDto item = result.getItems().get(0);
		Assertions.assertThat(item.getProductId()).isEqualTo(productId);
		Assertions.assertThat(item.getQuantity()).isEqualTo(2);
		Assertions.assertThat(item.getPrice()).isEqualByComparingTo("100.00");
		Assertions.assertThat(item.getSale()).isEqualByComparingTo("10.00");
		Assertions.assertThat(item.getTotalPrice()).isEqualByComparingTo("180.00");
	}

	@Test
	void toOrderDto_shouldMapMultipleEntitiesCorrectly() {
		// Given
		UUID orderId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		OrderEntity entity1 = OrderEntity.builder().orderId(orderId).userId(userId).productId(UUID.randomUUID())
				.quantity(1).price(new BigDecimal("50.00")).sale(new BigDecimal("0.00"))
				.totalPrice(new BigDecimal("50.00")).build();

		OrderEntity entity2 = OrderEntity.builder().orderId(orderId).userId(userId).productId(UUID.randomUUID())
				.quantity(3).price(new BigDecimal("30.00")).sale(new BigDecimal("5.00"))
				.totalPrice(new BigDecimal("75.00")) // (30-5)*3 = 75
				.build();

		List<OrderEntity> entities = Arrays.asList(entity1, entity2);

		// When
		OrderDto result = orderMapper.toOrderDto(entities);

		// Then
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result.getOrderId()).isEqualTo(orderId);
		Assertions.assertThat(result.getUserId()).isEqualTo(userId);
		Assertions.assertThat(result.getItems()).hasSize(2);
		Assertions.assertThat(result.getTotalPrice()).isEqualByComparingTo("125.00"); // 50 + 75
	}

	@Test
	void toOrderDto_shouldCalculateTotalPriceCorrectly() {
		// Given
		OrderEntity entity1 = OrderEntity.builder().orderId(UUID.randomUUID()).userId(UUID.randomUUID())
				.productId(UUID.randomUUID()).quantity(2).price(new BigDecimal("100.00")).sale(new BigDecimal("20.00"))
				.totalPrice(new BigDecimal("160.00")) // (100-20)*2
				.build();

		OrderEntity entity2 = OrderEntity.builder().orderId(UUID.randomUUID()).userId(UUID.randomUUID())
				.productId(UUID.randomUUID()).quantity(1).price(new BigDecimal("200.00")).sale(new BigDecimal("0.00"))
				.totalPrice(new BigDecimal("200.00")).build();

		List<OrderEntity> entities = Arrays.asList(entity1, entity2);

		// When
		OrderDto result = orderMapper.toOrderDto(entities);

		// Then
		Assertions.assertThat(result.getTotalPrice()).isEqualByComparingTo("360.00"); // 160 + 200
	}

	@Test
	void toOrderItemDto_shouldMapEntityCorrectly() {
		// Given
		UUID productId = UUID.randomUUID();
		OrderEntity entity = OrderEntity.builder().productId(productId).quantity(5).price(new BigDecimal("50.00"))
				.sale(new BigDecimal("5.00")).totalPrice(new BigDecimal("237.50")) // (50-2.5)*5
				.build();

		// When
		OrderItemDto result = orderMapper.toOrderItemDto(entity);

		// Then
		Assertions.assertThat(result.getProductId()).isEqualTo(productId);
		Assertions.assertThat(result.getQuantity()).isEqualTo(5);
		Assertions.assertThat(result.getPrice()).isEqualByComparingTo("50.00");
		Assertions.assertThat(result.getSale()).isEqualByComparingTo("5.00");
		Assertions.assertThat(result.getTotalPrice()).isEqualByComparingTo("237.50");
	}

	@Test
	void toOrderItemDto_shouldHandleNullValues() {
		// Given
		OrderEntity entity = OrderEntity.builder().productId(null).quantity(null).price(null).sale(null)
				.totalPrice(null).build();

		// When
		OrderItemDto result = orderMapper.toOrderItemDto(entity);

		// Then
		Assertions.assertThat(result.getProductId()).isNull();
		Assertions.assertThat(result.getQuantity()).isNull();
		Assertions.assertThat(result.getPrice()).isNull();
		Assertions.assertThat(result.getSale()).isNull();
		Assertions.assertThat(result.getTotalPrice()).isNull();
	}
}