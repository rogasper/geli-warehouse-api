package com.geli.warehouse.model;

import com.geli.warehouse.util.Timestamps;

import java.time.Instant;

import jakarta.persistence.*;


@Entity
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Item() {

    }

    public Item(String sku, String name, String description, long basePrice) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    @PrePersist
    void onCreate() {
        Instant now = Timestamps.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Timestamps.now();
    }

    public void updateDetails(String name, String description, long basePrice) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getBasePrice() { return basePrice; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt;}

}