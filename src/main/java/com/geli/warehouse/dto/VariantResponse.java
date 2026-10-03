package com.geli.warehouse.dto;

import java.time.Instant;

public record VariantResponse(
        Long id,
        Long itemId,
        String itemSku,
        String sku,
        String name,
        Long price,
        long effectivePrice,
        int stock,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
