package com.onlinestore.inventory.unit.mapper;

import java.math.BigDecimal;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.mapper.ProductMapper;

class ProductMapperTest {

	private final ProductMapper productMapper = new ProductMapper();

	@Test
	void toEntity_shouldMapCreateRequestToEntity() {
		CreateProductRequest request = CreateProductRequest.builder().name("Mapped Product")
				.price(new BigDecimal("79.99")).quantity(25).sale(new BigDecimal("5.00")).build();

		Product product = productMapper.toEntity(request);

		Assertions.assertThat(product).isNotNull();
		Assertions.assertThat(product.getName()).isEqualTo("Mapped Product");
		Assertions.assertThat(product.getPrice()).isEqualTo(new BigDecimal("79.99"));
		Assertions.assertThat(product.getQuantity()).isEqualTo(25);
		Assertions.assertThat(product.getSale()).isEqualTo(new BigDecimal("5.00"));
	}

	@Test
	void toResponse_shouldMapEntityToResponse() {
		Product product = Product.builder().id(UUID.fromString("35ce311d-d0e1-4572-b481-42bab1bd27ff"))
				.name("Response Product").price(new BigDecimal("199.99")).quantity(15).sale(new BigDecimal("20.00"))
				.build();

		ProductResponse response = productMapper.toResponse(product);

		Assertions.assertThat(response).isNotNull();
		Assertions.assertThat(response.getId()).isEqualTo(UUID.fromString("35ce311d-d0e1-4572-b481-42bab1bd27ff"));
		Assertions.assertThat(response.getName()).isEqualTo("Response Product");
		Assertions.assertThat(response.getPrice()).isEqualTo(new BigDecimal("199.99"));
		Assertions.assertThat(response.getQuantity()).isEqualTo(15);
		Assertions.assertThat(response.getSale()).isEqualTo(new BigDecimal("20.00"));
	}

	@Test
	void toResponse_shouldHandleNullSale() {
		Product product = Product.builder().id(UUID.fromString("35ce311d-d0e1-4572-b481-42bab1bd27ff"))
				.name("Product No Sale").price(new BigDecimal("100.00")).quantity(10).sale(null).build();

		ProductResponse response = productMapper.toResponse(product);

		Assertions.assertThat(response).isNotNull();
		Assertions.assertThat(response.getSale()).isNull();
	}
}
