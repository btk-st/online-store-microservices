package com.onlinestore.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false, length = 50)
    private String aggregateType; // "ORDER"

    @Column(nullable = false, length = 100)
    private String aggregateId;   // orderId.toString()

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;       // JSON события

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Builder.Default
    private int retryCount = 0;

    private String errorMessage;

    public enum EventType {
        ORDER_CREATED,
        ORDER_UPDATED,
        ORDER_CANCELLED
    }

    public enum EventStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED
    }
}
