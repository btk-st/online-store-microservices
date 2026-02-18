package com.onlinestore.inventory.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.repository.ProductRepository;
import com.onlinestore.inventory.service.api.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final ProductMapper productMapper;

	@Override
	public List<ProductResponse> getAllProducts() {
		log.info("Getting all products");
		return productRepository.findAll().stream().map(productMapper::toResponse).toList();
	}

	@Override
	public ProductResponse getProductById(UUID id) {
		log.info("Getting product by id: {}", id);
		Product product = findProductOrThrow(id);
		return productMapper.toResponse(product);
	}

	@Transactional
	@Override
	public ProductResponse createProduct(CreateProductRequest request) {
		log.info("Creating new product: {}", request.getName());

		Product product = productMapper.toEntity(request);
		Product savedProduct = productRepository.save(product);

		log.info("Product created with id: {}", savedProduct.getId());
		return productMapper.toResponse(savedProduct);
	}

	@Transactional
	@Override
	public void deleteProduct(UUID id) {
		log.info("Deleting product with id: {}", id);

		if (!productRepository.existsById(id)) {
			throw new ProductNotFoundException(id);
		}

		productRepository.deleteById(id);
		log.info("Product deleted: {}", id);
	}

	@Override
	public Product getProductEntityById(UUID id) {
		log.info("Getting product entity by id: {}", id);
		return findProductOrThrow(id);
	}

	private Product findProductOrThrow(UUID id) {
		return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}
}
