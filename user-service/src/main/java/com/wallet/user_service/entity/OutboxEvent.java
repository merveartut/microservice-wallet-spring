package com.wallet.user_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;


@Entity
@Table(name = "outbox_events", indexes = @Index(name = "ix_outbox_events_unpublished", columnList = "published_at, id"))
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    private String messageKey;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;
}