package com.onlinestore.inventory.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onlinestore.inventory.controller.api.ProductApi;
import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController implements ProductApi {

	private final ProductService productService;

	@GetMapping
	@Override
	public ResponseEntity<List<ProductResponse>> getAllProducts() {
		return ResponseEntity.ok(productService.getAllProducts());
	}

	@GetMapping("/{id}")
	@Override
	public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
		return ResponseEntity.ok(productService.getProductById(id));
	}

	@PostMapping
	@Override
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
		ProductResponse response = productService.createProduct(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/{id}")
	@Override
	public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}
}
