package com.qify.fulfillment.domain;

import java.time.Instant;
import java.util.UUID;

import com.qify.identity.domain.Actor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "request_timeline")
public class RequestTimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private QueueRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_actor_id", nullable = false)
    private Actor performedByActor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected RequestTimelineEntry() {
    }

    public Long getId() {
        return id;
    }

    public QueueRequest getRequest() {
        return request;
    }

    public QueueRequestStatus getStatus() {
        return status;
    }

    public Actor getPerformedByActor() {
        return performedByActor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
