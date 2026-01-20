package com.onlinestore.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for creating or updating a product")
@Builder
public class CreateProductRequest {

  @NotBlank(message = "Product name is required")
  @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
  @Schema(
      description = "Product name",
      example = "iPhone 15 Pro",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @NotNull(message = "Quantity is required")
  @Min(value = 0, message = "Quantity cannot be negative")
  @Schema(
      description = "Available quantity in stock",
      example = "10",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer quantity;

  @NotNull(message = "Price is required")
  @DecimalMin(value = "0.01", message = "Price must be greater than 0")
  @Schema(
      description = "Product price",
      example = "1299.99",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal price;

  @DecimalMin(value = "0.00", message = "Sale cannot be negative")
  @DecimalMax(value = "100.00", message = "Sale cannot exceed 100%")
  @Schema(description = "Discount percentage", example = "15.50")
  private BigDecimal sale;
}
