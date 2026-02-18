package com.onlinestore.order.exception;

/**
 * gRPC ошибка
 */
public class InventoryServiceException extends RuntimeException {
	public InventoryServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
