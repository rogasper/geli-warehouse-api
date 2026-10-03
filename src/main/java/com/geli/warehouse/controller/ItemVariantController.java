package com.geli.warehouse.controller;

import com.geli.warehouse.dto.VariantCreateRequest;
import com.geli.warehouse.dto.VariantResponse;
import com.geli.warehouse.service.VariantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/items/{itemId}/variants")
public class ItemVariantController {
    private final VariantService variantService;

    public ItemVariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping
    public ResponseEntity<VariantResponse> create(
            @PathVariable Long itemId,
            @Valid @RequestBody VariantCreateRequest request
            ){
        VariantResponse created = variantService.create(itemId, request);
        return ResponseEntity.created(URI.create("/api/v1/variants/" + created.id())).body(created);
    }

    @GetMapping
    public List<VariantResponse> list(@PathVariable Long itemId){
        return variantService.listByItem(itemId);
    }
}
