package com.geli.warehouse.service;

import com.geli.warehouse.dto.ItemCreateRequest;
import com.geli.warehouse.dto.ItemResponse;
import com.geli.warehouse.dto.ItemUpdateRequest;
import com.geli.warehouse.dto.PageResponse;
import com.geli.warehouse.exception.ConflictException;
import com.geli.warehouse.exception.NotFoundException;
import com.geli.warehouse.model.Item;
import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.repository.VariantRepository;
import com.geli.warehouse.repository.VariantRepository.VariantSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;

    public ItemService(ItemRepository itemRepository, VariantRepository variantRepository){
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> list(String q, boolean active, Pageable pageable){
        String query = (q == null || q.isBlank() ? null : q.trim());
        Page<Item> page = itemRepository.search(query, active, pageable);
        Map<Long, VariantSummary> summaries = summarize(page.getContent());
        return PageResponse.of(page.map(item -> toResponse(item, summaries.get(item.getId()))));
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id){
        Item item = itemRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Item", id));
        return toResponse(item, summarize(List.of(item)).get(id));
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request){
        if(itemRepository.existsBySku(request.sku())){
            throw new ConflictException("Item with SKU '"+ request.sku()+"' already exists");
        }
        Item item = new Item(request.sku(), request.name(), request.description(), request.basePrice());
        return toResponse(itemRepository.save(item), null);
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest request){
        Item item = itemRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Item", id));
        item.updateDetails(request.name(), request.description(), request.basePrice());
        return toResponse(item, summarize(List.of(item)).get(id));
    }

    @Transactional
    public void delete(Long id){
        Item item = itemRepository.findByIdAndActiveTrue(id)
                .orElseThrow(()->new NotFoundException("Item", id));
        item.deactivate();
    }

    private Map<Long, VariantSummary> summarize(List<Item> items){
        if(items.isEmpty()){
            return Map.of();
        }
        List<Long> itemIds = items.stream().map(Item::getId).toList();
        return variantRepository.summarizeByItemIds(itemIds).stream()
                .collect(Collectors.toMap(VariantSummary::getItemId, Function.identity()));
    }

    private ItemResponse toResponse(Item item, VariantSummary summary){
        int variantCount = summary == null ? 0 : (int) summary.getVariantCount();
        int totalStock = summary == null ? 0 : (int) summary.getTotalStock();
        return new ItemResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getDescription(),
                item.getBasePrice(),
                item.isActive(),
                variantCount,
                totalStock,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
