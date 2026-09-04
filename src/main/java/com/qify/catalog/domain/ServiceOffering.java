package com.qify.catalog.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "service_offerings", uniqueConstraints = @UniqueConstraint(columnNames = {"venue_id", "category"}))
public class ServiceOffering {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceCategory category;

    @Column(name = "delegation_approved", nullable = false)
    private boolean delegationApproved;

    @Column(nullable = false)
    private boolean active;

    protected ServiceOffering() {
    }

    public UUID getId() {
        return id;
    }

    public Venue getVenue() {
        return venue;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public boolean isDelegationApproved() {
        return delegationApproved;
    }

    public boolean isActive() {
        return active;
    }
}
