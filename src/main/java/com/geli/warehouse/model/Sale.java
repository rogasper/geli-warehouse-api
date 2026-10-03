package com.geli.warehouse.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SaleLine> lines = new ArrayList<>();

    public Sale(){

    }

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }

    public void addLine(Variant variant, int quantity, long unitPrice){
        SaleLine line = new SaleLine(this, variant, quantity, unitPrice);
        lines.add(line);
        this.totalAmount += line.getLineTotal();
    }

    public Long getId() { return id; }
    public Long getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public List<SaleLine> getLines() { return lines; }
}
