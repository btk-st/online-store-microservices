package com.onlinestore.inventory.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response")
public class ErrorResponse {

  @Schema(
          description = "Timestamp when error occurred",
          example = "2024-01-15 14:30:45",
          type = "string"
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime timestamp;

  @Schema(
          description = "HTTP status code",
          example = "404"
  )
  private int status;

  @Schema(
          description = "HTTP error reason",
          example = "Not Found"
  )
  private String error;

  @Schema(
          description = "Detailed error message",
          example = "Product with id 123 not found"
  )
  private String message;
}
