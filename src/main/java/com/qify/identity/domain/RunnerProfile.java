package com.qify.identity.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "runner_profiles")
public class RunnerProfile {

    @Id
    private UUID id;

    @OneToOne
    @JoinColumn(name = "actor_id", nullable = false, unique = true)
    private Actor actor;

    @Column(nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunnerAvailability availability;

    protected RunnerProfile() {
    }

    public UUID getId() {
        return id;
    }

    public Actor getActor() {
        return actor;
    }

    public boolean isVerified() {
        return verified;
    }

    public RunnerAvailability getAvailability() {
        return availability;
    }
}
