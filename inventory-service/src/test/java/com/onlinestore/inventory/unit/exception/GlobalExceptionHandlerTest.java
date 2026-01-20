package com.onlinestore.inventory.unit.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onlinestore.inventory.exception.ErrorResponse;
import com.onlinestore.inventory.exception.GlobalExceptionHandler;
import com.onlinestore.inventory.exception.ProductNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

  @Test
  void handleProductNotFound_shouldReturn404WithErrorResponse() {

    UUID productId = UUID.randomUUID();
    ProductNotFoundException ex = new ProductNotFoundException(productId);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleProductNotFound(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(404);
    assertThat(response.getBody().getError()).isEqualTo("Not Found");
    assertThat(response.getBody().getMessage()).contains(productId.toString());
    assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  void handleValidationExceptions_shouldReturn400WithValidationErrors() {

    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);

    FieldError fieldError1 = new FieldError("object", "name", "Name is required");
    FieldError fieldError2 = new FieldError("object", "price", "Price must be positive");

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

    ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationExceptions(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull().containsEntry("status", 400);
    assertThat(body.get("timestamp")).isNotNull();

    @SuppressWarnings("unchecked")
    Map<String, String> errors = (Map<String, String>) body.get("errors");
    assertThat(errors)
        .hasSize(2)
        .containsEntry("name", "Name is required")
        .containsEntry("price", "Price must be positive");
  }

  @Test
  void handleGenericException_shouldReturn500WithErrorMessage() {
    // Arrange
    Exception ex = new RuntimeException("Something went wrong");

    // Act
    ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(500);
    assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
    assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
  }

  @Test
  void handleGenericException_shouldHandleNullMessage() {

    Exception ex = new RuntimeException(); // без сообщения

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertNotNull(response.getBody());
    assertThat(response.getBody().getMessage()).isNull();
  }
}
