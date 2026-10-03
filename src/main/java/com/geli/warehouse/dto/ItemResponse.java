package com.geli.warehouse.dto;

import java.time.Instant;

public record ItemResponse(
        Long id,
        String sku,
        String name,
        String description,
        long basePrice,
        boolean active,
        int variantCount,
        int totalStock,
        Instant createdAt,
        Instant updatedAt) {
}
