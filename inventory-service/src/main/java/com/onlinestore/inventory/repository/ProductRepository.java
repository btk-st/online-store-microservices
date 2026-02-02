package com.onlinestore.inventory.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onlinestore.inventory.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
}
