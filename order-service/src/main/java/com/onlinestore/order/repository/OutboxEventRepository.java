package com.onlinestore.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onlinestore.order.entity.OutboxEvent;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	List<OutboxEvent> findByStatus(OutboxEvent.EventStatus status);
}
