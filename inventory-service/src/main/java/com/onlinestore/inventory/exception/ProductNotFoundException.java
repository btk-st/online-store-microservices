package com.onlinestore.inventory.exception;

import java.util.UUID;

/**
 * Выбрасывается при попытке найти или удалить несуществующий продукт
 */
public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException(UUID id) {
		super("Product not found with id: " + id);
	}
}
