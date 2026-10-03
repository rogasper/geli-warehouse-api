package com.geli.warehouse.controller;

import com.geli.warehouse.dto.ItemCreateRequest;
import com.geli.warehouse.dto.ItemResponse;
import com.geli.warehouse.dto.ItemUpdateRequest;
import com.geli.warehouse.dto.PageResponse;
import com.geli.warehouse.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {
    private static final int MAX_PAGE_SIZE = 100;
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest request){
        ItemResponse created = itemService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/items/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<ItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "true") boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("id").ascending()
        );
        return itemService.list(q, active, pageable);
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id){
        return itemService.get(id);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemUpdateRequest request){
        return itemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        itemService.delete(id);
    }
}
