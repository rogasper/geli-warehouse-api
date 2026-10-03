package com.geli.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VariantCreateRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 255) String name,
        @PositiveOrZero Long price,
        @PositiveOrZero Integer initialStock) {
}
