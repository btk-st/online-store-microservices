package com.onlinestore.inventory.service.api;

import java.util.List;
import java.util.UUID;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;

public interface ProductService {
	List<ProductResponse> getAllProducts();
	ProductResponse getProductById(UUID id);
	ProductResponse createProduct(CreateProductRequest request);
	void deleteProduct(UUID id);
	Product getProductEntityById(UUID id);
}
