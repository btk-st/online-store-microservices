package com.onlinestore.order.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "outbox_events")
public class OutboxEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String payload; // JSON события

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private EventStatus status = EventStatus.PENDING;

	@Builder.Default
	private int retryCount = 0;

	public enum EventStatus {
		PENDING, PROCESSING, PROCESSED, FAILED
	}
}
