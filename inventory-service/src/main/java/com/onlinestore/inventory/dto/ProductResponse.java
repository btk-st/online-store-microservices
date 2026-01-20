package com.onlinestore.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product information")
public class ProductResponse {

  @Schema(
      description = "Unique product identifier",
      example = "123e4567-e89b-12d3-a456-426614174000")
  private UUID id;

  @Schema(description = "Product name", example = "iPhone 15 Pro")
  private String name;

  @Schema(description = "Available quantity in stock", example = "10")
  private Integer quantity;

  @Schema(description = "Product price", example = "1299.99")
  private BigDecimal price;

  @Schema(description = "Discount percentage", example = "15.50")
  private BigDecimal sale;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "Creation timestamp", example = "2024-01-20 10:30:00", format = "date-time")
  private LocalDateTime createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(
      description = "Last update timestamp",
      example = "2024-01-20 11:45:00",
      format = "date-time")
  private LocalDateTime updatedAt;
}
