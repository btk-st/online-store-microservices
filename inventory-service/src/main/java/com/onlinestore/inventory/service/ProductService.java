package com.onlinestore.inventory.service;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import com.onlinestore.inventory.mapper.ProductMapper;
import com.onlinestore.inventory.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public List<ProductResponse> getAllProducts() {
    log.info("Getting all products");
    return productRepository.findAll().stream().map(productMapper::toResponse).toList();
  }

  public ProductResponse getProductById(UUID id) {
    log.info("Getting product by id: {}", id);
    Product product = findProductOrThrow(id);
    return productMapper.toResponse(product);
  }

  @Transactional
  public ProductResponse createProduct(CreateProductRequest request) {
    log.info("Creating new product: {}", request.getName());

    Product product = productMapper.toEntity(request);
    Product savedProduct = productRepository.save(product);

    log.info("Product created with id: {}", savedProduct.getId());
    return productMapper.toResponse(savedProduct);
  }

  @Transactional
  public ProductResponse updateProduct(UUID id, CreateProductRequest request) {
    log.info("Updating product with id: {}", id);

    Product product = findProductOrThrow(id);
    product.setName(request.getName());
    product.setQuantity(request.getQuantity());
    product.setPrice(request.getPrice());
    product.setSale(request.getSale() != null ? request.getSale() : product.getSale());

    Product updatedProduct = productRepository.save(product);
    log.info("Product updated: {}", updatedProduct.getId());

    return productMapper.toResponse(updatedProduct);
  }

  @Transactional
  public void deleteProduct(UUID id) {
    log.info("Deleting product with id: {}", id);

    if (!productRepository.existsById(id)) {
      throw new ProductNotFoundException(id);
    }

    productRepository.deleteById(id);
    log.info("Product deleted: {}", id);
  }

  private Product findProductOrThrow(UUID id) {
    return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
  }

  public Product getProductEntityById(UUID id) {
    log.info("Getting product entity by id: {}", id);
    return findProductOrThrow(id);
  }
}
