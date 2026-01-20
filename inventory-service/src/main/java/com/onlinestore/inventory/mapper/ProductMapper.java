package com.onlinestore.inventory.mapper;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

  public Product toEntity(CreateProductRequest request) {
    return Product.builder()
        .name(request.getName())
        .quantity(request.getQuantity())
        .price(request.getPrice())
        .sale(request.getSale() != null ? request.getSale() : BigDecimal.ZERO)
        .build();
  }

  public ProductResponse toResponse(Product product) {
    return ProductResponse.builder()
        .id(product.getId())
        .name(product.getName())
        .quantity(product.getQuantity())
        .price(product.getPrice())
        .sale(product.getSale())
        .createdAt(product.getCreatedAt())
        .updatedAt(product.getUpdatedAt())
        .build();
  }
}
