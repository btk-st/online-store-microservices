package com.onlinestore.inventory.unit.mapper;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper productMapper = new ProductMapper();

    @Test
    void toEntity_shouldMapCreateRequestToEntity() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Mapped Product")
                .price(new BigDecimal("79.99"))
                .quantity(25)
                .sale(new BigDecimal("5.00"))
                .build();

        Product product = productMapper.toEntity(request);

        assertThat(product).isNotNull();
        assertThat(product.getName()).isEqualTo("Mapped Product");
        assertThat(product.getPrice()).isEqualTo(new BigDecimal("79.99"));
        assertThat(product.getQuantity()).isEqualTo(25);
        assertThat(product.getSale()).isEqualTo(new BigDecimal("5.00"));
    }

    @Test
    void toResponse_shouldMapEntityToResponse() {
        Product product = Product.builder()
                .id(UUID.fromString("35ce311d-d0e1-4572-b481-42bab1bd27ff"))
                .name("Response Product")
                .price(new BigDecimal("199.99"))
                .quantity(15)
                .sale(new BigDecimal("20.00"))
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(UUID.fromString("35ce311d-d0e1-4572-b481-42bab1bd27ff"));
        assertThat(response.getName()).isEqualTo("Response Product");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("199.99"));
        assertThat(response.getQuantity()).isEqualTo(15);
        assertThat(response.getSale()).isEqualTo(new BigDecimal("20.00"));
    }

    @Test
    void toResponse_shouldHandleNullSale() {
        Product product = Product.builder()
                .id(UUID.fromString("35ce311d-d0e1-4572-b481-42bab1bd27ff"))
                .name("Product No Sale")
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .sale(null)
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertThat(response).isNotNull();
        assertThat(response.getSale()).isNull();
    }
}
