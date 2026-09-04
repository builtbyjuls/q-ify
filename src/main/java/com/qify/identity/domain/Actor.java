package com.qify.identity.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "actors")
public class Actor {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActorRole role;

    protected Actor() {
    }

    public UUID getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public ActorRole getRole() {
        return role;
    }
}
