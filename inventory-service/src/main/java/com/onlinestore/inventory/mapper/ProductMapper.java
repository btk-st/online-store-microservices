package com.onlinestore.inventory.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.grpc.ProductAvailabilityResponse;

@Component
public class ProductMapper {

	public Product toEntity(CreateProductRequest request) {
		return Product.builder().name(request.getName()).quantity(request.getQuantity()).price(request.getPrice())
				.sale(request.getSale() != null ? request.getSale() : BigDecimal.ZERO).build();
	}

	public ProductResponse toResponse(Product product) {
		return ProductResponse.builder().id(product.getId()).name(product.getName()).quantity(product.getQuantity())
				.price(product.getPrice()).sale(product.getSale()).build();
	}

	public ProductAvailabilityResponse toAvailabilityResponse(Product product) {
		double discount = product.getSale() != null ? product.getSale().doubleValue() : 0.0;
		return ProductAvailabilityResponse.newBuilder().setProductId(product.getId().toString())
				.setProductName(product.getName()).setPrice(product.getPrice().doubleValue()).setDiscount(discount)
				.setAvailableQuantity(product.getQuantity()).build();
	}

	public ProductAvailabilityResponse toFailedAvailabilityResponse(String productId) {
		return ProductAvailabilityResponse.newBuilder().setProductId(productId).setIsAvailable(false)
				.setMessage("Product not found").build();
	}
}
