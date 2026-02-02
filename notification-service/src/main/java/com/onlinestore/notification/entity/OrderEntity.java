package com.onlinestore.notification.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "product_id")
	private UUID productId;

	@Column(name = "quantity")
	private Integer quantity;

	@Column(name = "price", precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "sale", precision = 5, scale = 2)
	@Builder.Default
	private BigDecimal sale = BigDecimal.ZERO;

	@Column(name = "total_price", precision = 10, scale = 2)
	private BigDecimal totalPrice;

	@Column(name = "user_id", nullable = false)
	private UUID userId;
}
