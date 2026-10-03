package com.geli.warehouse.model;


import jakarta.persistence.*;

import com.geli.warehouse.util.Timestamps;

import java.time.Instant;

@Entity
@Table(name = "variants")
public class Variant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    private Long price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Variant(){
    }

    public Variant(Item item, String sku, String name, Long price, int stock){
        this.item = item;
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    @PrePersist
    void onCreate(){
        Instant now = Timestamps.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Timestamps.now();
    }

    public long effectivePrice(){
        return price != null ? price : item.getBasePrice();
    }

    public void updateDetails(String name, Long price){
        this.name = name;
        this.price = price;
    }

    public void deactivate(){
        this.active = false;
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Long getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
