package com.qify.catalog.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "venues")
public class Venue {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    protected Venue() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
