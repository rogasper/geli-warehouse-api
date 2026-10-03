package com.geli.warehouse.controller;

import com.geli.warehouse.dto.PageResponse;
import com.geli.warehouse.dto.SaleCreateRequest;
import com.geli.warehouse.dto.SaleResponse;
import com.geli.warehouse.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {
    private static final int MAX_PAGE_SIZE = 100;
    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleCreateRequest request){
        SaleResponse created = saleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sales/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<SaleResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return saleService.list(pageable);
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable Long id){
        return saleService.get(id);
    }
}
