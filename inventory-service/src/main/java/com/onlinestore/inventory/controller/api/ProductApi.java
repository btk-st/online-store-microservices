package com.onlinestore.inventory.controller.api;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Products", description = "API for product inventory management")
@RequestMapping("/api/products")
public interface ProductApi {

    @Operation(summary = "Get all products", description = "Returns a list of all products in the inventory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class)))
    })
    ResponseEntity<List<ProductResponse>> getAllProducts();


    @Operation(summary = "Get product by ID", description = "Returns a single product by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "400", description = "Invalid UUID format")
    })
    ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product UUID", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            UUID id);


    @Operation(summary = "Create new product", description = "Creates a new product in the inventory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    ResponseEntity<ProductResponse> createProduct(CreateProductRequest request);


    @Operation(summary = "Delete product", description = "Deletes a product from the inventory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @ApiResponse(responseCode = "404", description = "Product not found")})
    ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product UUID", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            UUID id);
}
