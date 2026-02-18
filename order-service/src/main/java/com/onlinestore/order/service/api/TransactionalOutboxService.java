package com.onlinestore.order.service.api;

import com.onlinestore.order.entity.Order;

/**
 * Сервис для работы с outbox.
 */
public interface TransactionalOutboxService {
	/**
	 * Сохраняет событие о созданном заказе в outbox таблицу.
	 * <p>
	 * <b>Важно:</b> Должен вызываться в рамках той же транзакции, что и сохранение
	 * заказа (JPA гарантирует атомарность).
	 * </p>
	 *
	 * @param order
	 *            созданный заказ (должен быть уже сохранен в БД)
	 * @throws RuntimeException
	 *             если сериализация события в JSON не удалась
	 */
	void saveOrderCreatedEvent(Order order);
}
