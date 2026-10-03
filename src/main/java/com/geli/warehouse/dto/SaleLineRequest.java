package com.geli.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleLineRequest(
        @NotNull Long variantId,
        @NotNull @Min(1) Integer quantity) {
}
