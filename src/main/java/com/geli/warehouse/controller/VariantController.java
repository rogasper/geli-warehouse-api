package com.geli.warehouse.controller;

import com.geli.warehouse.dto.*;
import com.geli.warehouse.service.VariantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/variants")
public class VariantController {
    private static final int MAX_PAGE_SIZE = 100;
    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping("/{id}")
    public VariantResponse get(@PathVariable Long id){
        return variantService.get(id);
    }

    @PutMapping("/{id}")
    public VariantResponse update(@PathVariable Long id, @Valid @RequestBody VariantUpdateRequest request){
        return variantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        variantService.delete(id);
    }

    @PostMapping("/{id}/stock-adjustments")
    public VariantResponse adjustStock(@PathVariable Long id,
                                       @Valid @RequestBody StockAdjustmentRequest request){
        return variantService.adjustStock(id, request);
    }

    @GetMapping("/{id}/stock-movements")
    public PageResponse<StockMovementResponse> movements(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size,1), MAX_PAGE_SIZE));
        return variantService.movements(id, pageable);
    }
}

