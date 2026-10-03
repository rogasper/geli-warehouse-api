package com.geli.warehouse.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private Variant variant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MovementType type;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement(){}

    public StockMovement(Variant variant, MovementType type, int quantityDelta, int stockAfter, Long referenceId, String reason) {
        this.variant = variant;
        this.type = type;
        this.quantityDelta = quantityDelta;
        this.stockAfter = stockAfter;
        this.referenceId = referenceId;
        this.reason = reason;
    }

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Variant getVariant() {
        return variant;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}


