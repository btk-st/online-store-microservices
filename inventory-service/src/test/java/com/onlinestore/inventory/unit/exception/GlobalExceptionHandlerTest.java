package com.onlinestore.inventory.unit.exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.onlinestore.inventory.exception.ErrorResponse;
import com.onlinestore.inventory.exception.GlobalExceptionHandler;
import com.onlinestore.inventory.exception.ProductNotFoundException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

	@Test
	void handleProductNotFound_shouldReturn404WithErrorResponse() {

		UUID productId = UUID.randomUUID();
		ProductNotFoundException ex = new ProductNotFoundException(productId);

		ResponseEntity<ErrorResponse> response = exceptionHandler.handleProductNotFound(ex);

		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		Assertions.assertThat(response.getBody()).isNotNull();
		Assertions.assertThat(response.getBody().getStatus()).isEqualTo(404);
		Assertions.assertThat(response.getBody().getError()).isEqualTo("Not Found");
		Assertions.assertThat(response.getBody().getMessage()).contains(productId.toString());
		Assertions.assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
	}

	@Test
	void handleValidationExceptions_shouldReturn400WithValidationErrors() {

		MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
		BindingResult bindingResult = Mockito.mock(BindingResult.class);

		FieldError fieldError1 = new FieldError("object", "name", "Name is required");
		FieldError fieldError2 = new FieldError("object", "price", "Price must be positive");

		Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
		Mockito.when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

		ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationExceptions(ex);

		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		Map<String, Object> body = response.getBody();
		Assertions.assertThat(body).isNotNull().containsEntry("status", 400);
		Assertions.assertThat(body.get("timestamp")).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, String> errors = (Map<String, String>) body.get("errors");
		Assertions.assertThat(errors).hasSize(2).containsEntry("name", "Name is required").containsEntry("price",
				"Price must be positive");
	}

	@Test
	void handleGenericException_shouldReturn500WithErrorMessage() {
		// Arrange
		Exception ex = new RuntimeException("Something went wrong");

		// Act
		ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

		// Assert
		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		Assertions.assertThat(response.getBody()).isNotNull();
		Assertions.assertThat(response.getBody().getStatus()).isEqualTo(500);
		Assertions.assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
		Assertions.assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
	}

	@Test
	void handleGenericException_shouldHandleNullMessage() {

		Exception ex = new RuntimeException(); // без сообщения

		ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		Assertions.assertThat(response.getBody()).isNotNull();
		Assertions.assertThat(response.getBody().getMessage()).isNull();
	}
}
