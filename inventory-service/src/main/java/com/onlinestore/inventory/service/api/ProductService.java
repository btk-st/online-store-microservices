package com.onlinestore.inventory.service.api;

import java.util.List;
import java.util.UUID;

import com.onlinestore.inventory.dto.CreateProductRequest;
import com.onlinestore.inventory.dto.ProductResponse;
import com.onlinestore.inventory.entity.Product;
import com.onlinestore.inventory.exception.ProductNotFoundException;

public interface ProductService {
	/**
	 * Возвращает все товары.
	 * 
	 * @return список товаров (может быть пустым)
	 */
	List<ProductResponse> getAllProducts();

	/**
	 * Возвращает товар по его id.
	 * 
	 * @param id
	 *            UUID товара
	 * @return DTO товара
	 * @throws ProductNotFoundException
	 *             если товар с таким id не найден
	 */
	ProductResponse getProductById(UUID id);

	/**
	 * Создает новый товар.
	 * 
	 * @param request
	 *            данные для создания товара
	 * @return созданный товар с присвоенным id
	 */
	ProductResponse createProduct(CreateProductRequest request);

	/**
	 * Удаляет товар по id.
	 * 
	 * @param id
	 *            UUID товара
	 * @throws ProductNotFoundException
	 *             если товар не найден
	 */
	void deleteProduct(UUID id);

	/**
	 * Возвращает entity товара для внутреннего использования. Метод не для REST
	 * API.
	 * 
	 * @param id
	 *            UUID товара
	 * @return entity товара
	 * @throws ProductNotFoundException
	 *             если товар не найден
	 */
	Product getProductEntityById(UUID id);
}
