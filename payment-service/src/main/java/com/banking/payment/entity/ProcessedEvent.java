package com.banking.payment.entity;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    @Column(name = "event_id")
    private String eventId;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
