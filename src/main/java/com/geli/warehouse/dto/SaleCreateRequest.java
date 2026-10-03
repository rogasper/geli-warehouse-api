package com.geli.warehouse.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SaleCreateRequest(
        @NotEmpty List<@Valid SaleLineRequest> lines) {
}
