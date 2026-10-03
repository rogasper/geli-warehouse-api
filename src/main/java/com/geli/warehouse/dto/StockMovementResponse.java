package com.geli.warehouse.dto;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long variantId,
        String type,
        int quantityDelta,
        int stockAfter,
        Long referenceId,
        String reason,
        Instant createdAt) {
}
