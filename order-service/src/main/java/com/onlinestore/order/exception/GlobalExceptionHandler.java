package com.onlinestore.order.exception;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Обработка ProductNotAvailableException
    @ExceptionHandler(ProductNotAvailableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleProductNotAvailable(ProductNotAvailableException ex) {
        log.warn("Product not available: {}", ex.getMessage());

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(ex.getMessage())
                .details(Map.of(
                        "productId", ex.getProductId().toString(),
                        "requestedQuantity", ex.getRequestedQuantity(),
                        "availableQuantity", ex.getAvailableQuantity()
                ))
                .build();
    }

    // Обработка валидации (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // ← 400
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .details(errors)
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // ← 400
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message(ex.getMessage())
                .build();
    }

    // DTO для ответа
    @Builder
    @Data
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private Map<String, Object> details;
    }
}
