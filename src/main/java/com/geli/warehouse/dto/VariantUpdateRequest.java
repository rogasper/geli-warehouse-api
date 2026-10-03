package com.geli.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** A null {@code price} clears the override so the item's base price applies. */
public record VariantUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @PositiveOrZero Long price) {
}
