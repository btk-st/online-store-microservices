package com.onlinestore.inventory.controller;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "API for product inventory management")
public class ProductController {

  private final ProductService productService;

  @Operation(
      summary = "Get all products",
      description = "Returns a list of all products in the inventory")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved products",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @GetMapping
  public ResponseEntity<List<ProductResponse>> getAllProducts() {
    return ResponseEntity.ok(productService.getAllProducts());
  }

  @Operation(
      summary = "Get product by ID",
      description = "Returns a single product by its unique identifier")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "400", description = "Invalid UUID format")
      })
  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getProductById(
      @Parameter(
              description = "Product UUID",
              required = true,
              example = "123e4567-e89b-12d3-a456-426614174000",
              schema = @Schema(type = "string", format = "uuid"))
          @PathVariable
          UUID id) {
    return ResponseEntity.ok(productService.getProductById(id));
  }

  @Operation(summary = "Create new product", description = "Creates a new product in the inventory")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Product created successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PostMapping
  public ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request) {
    ProductResponse response = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Update existing product",
      description = "Updates an existing product by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> updateProduct(
      @Parameter(
              description = "Product UUID",
              required = true,
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID id,
      @Valid @RequestBody CreateProductRequest request) {
    return ResponseEntity.ok(productService.updateProduct(id, request));
  }

  @Operation(summary = "Delete product", description = "Deletes a product from the inventory")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found")
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(
      @Parameter(
              description = "Product UUID",
              required = true,
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          UUID id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }
}
