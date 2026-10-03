package com.geli.warehouse.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sale_lines")
public class SaleLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private Variant variant;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    protected SaleLine(){}

    SaleLine(Sale sale, Variant variant, int quantity, long unitPrice) {
        this.sale = sale;
        this.variant = variant;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal =unitPrice*quantity;
    }

    public Long getId() {
        return id;
    }

    public Sale getSale() {
        return sale;
    }

    public Variant getVariant() {
        return variant;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public long getLineTotal() {
        return lineTotal;
    }
}
