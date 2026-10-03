package com.geli.warehouse.dto;

public record SaleLineResponse(
        Long variantId,
        String variantSku,
        int quantity,
        long unitPrice,
        long lineTotal) {
}
