package com.onlinestore.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onlinestore.order.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

	List<Order> findByUserId(UUID userId);
	boolean existsByIdAndUserId(UUID orderId, UUID userId);
}
