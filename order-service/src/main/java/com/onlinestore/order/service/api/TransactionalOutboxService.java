package com.onlinestore.order.service.api;

import com.onlinestore.order.entity.Order;

public interface TransactionalOutboxService {
	void saveOrderCreatedEvent(Order order);
}
