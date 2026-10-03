package com.geli.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code quantityDelta} must not be zero; the service enforces that. */
public record StockAdjustmentRequest(
        @NotNull Integer quantityDelta,
        @NotBlank @Size(max = 255) String reason) {
}
