package com.onlinestore.notification.repository;

import com.onlinestore.notification.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByOrderId(UUID orderId);
    List<OrderEntity> findByUserId(UUID userId);
    @Query("SELECT DISTINCT o.orderId FROM OrderEntity o")
    List<UUID> findAllOrderIds();
    @Query("SELECT DISTINCT o.orderId FROM OrderEntity o WHERE o.userId = :userId")
    List<UUID> findOrderIdsByUserId(UUID userId);

    boolean existsByOrderIdAndProductId(UUID orderId, UUID productId);
}
