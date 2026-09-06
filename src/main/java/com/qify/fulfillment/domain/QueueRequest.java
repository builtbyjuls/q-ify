package com.qify.fulfillment.domain;

import java.time.Instant;
import java.util.UUID;

import com.qify.catalog.domain.ServiceOffering;
import com.qify.identity.domain.Actor;
import com.qify.identity.domain.ActorRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "queue_requests")
public class QueueRequest {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Actor customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_offering_id", nullable = false)
    private ServiceOffering serviceOffering;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueRequestStatus status;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "expected_queue_minutes", nullable = false)
    private int expectedQueueMinutes;

    @Column(name = "arrival_notice_minutes", nullable = false)
    private int arrivalNoticeMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QueueRequest() {
    }

    public static QueueRequest create(UUID id, Actor customer, ServiceOffering serviceOffering,
            Instant scheduledFor, int expectedQueueMinutes, int arrivalNoticeMinutes, Instant now) {
        if (id == null || customer == null || serviceOffering == null || scheduledFor == null || now == null) {
            throw new InvalidQueueRequestCreationException("Required queue request value is missing");
        }
        if (customer.getRole() != ActorRole.CUSTOMER) {
            throw new InvalidQueueRequestCreationException("Queue request customer must have CUSTOMER role");
        }
        if (!serviceOffering.isActive()) {
            throw new InvalidQueueRequestCreationException("Service offering must be active");
        }
        if (!serviceOffering.isDelegationApproved()) {
            throw new InvalidQueueRequestCreationException("Service offering must be delegation approved");
        }
        if (!scheduledFor.isAfter(now)) {
            throw new InvalidQueueRequestCreationException("Scheduled time must be after now");
        }
        if (expectedQueueMinutes < 1 || expectedQueueMinutes > 720) {
            throw new InvalidQueueRequestCreationException("Expected queue minutes must be between 1 and 720");
        }
        if (arrivalNoticeMinutes < 0 || arrivalNoticeMinutes > 120) {
            throw new InvalidQueueRequestCreationException("Arrival notice minutes must be between 0 and 120");
        }

        QueueRequest request = new QueueRequest();
        request.id = id;
        request.customer = customer;
        request.serviceOffering = serviceOffering;
        request.status = QueueRequestStatus.REQUESTED;
        request.scheduledFor = scheduledFor;
        request.expectedQueueMinutes = expectedQueueMinutes;
        request.arrivalNoticeMinutes = arrivalNoticeMinutes;
        request.createdAt = now;
        return request;
    }

    public UUID getId() {
        return id;
    }

    public Actor getCustomer() {
        return customer;
    }

    public ServiceOffering getServiceOffering() {
        return serviceOffering;
    }

    public QueueRequestStatus getStatus() {
        return status;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public int getExpectedQueueMinutes() {
        return expectedQueueMinutes;
    }

    public int getArrivalNoticeMinutes() {
        return arrivalNoticeMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
