package com.geli.warehouse.dto;

import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        Instant createdAt,
        long totalAmount,
        List<SaleLineResponse> lines) {
}
