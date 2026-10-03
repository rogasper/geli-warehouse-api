package com.geli.warehouse.service;

import com.geli.warehouse.dto.*;
import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.exception.ConflictException;
import com.geli.warehouse.exception.NotFoundException;
import com.geli.warehouse.model.Item;
import com.geli.warehouse.model.MovementType;
import com.geli.warehouse.model.StockMovement;
import com.geli.warehouse.model.Variant;
import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.repository.StockMovementRepository;
import com.geli.warehouse.repository.VariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VariantService {
    private final VariantRepository variantRepository;
    private final ItemRepository itemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockLedgerService stockLedgerService;

    public VariantService(VariantRepository variantRepository, ItemRepository itemRepository, StockMovementRepository stockMovementRepository, StockLedgerService stockLedgerService) {
        this.variantRepository = variantRepository;
        this.itemRepository = itemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockLedgerService = stockLedgerService;
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listByItem(Long itemId){
        itemRepository.findByIdAndActiveTrue(itemId)
                .orElseThrow(() -> new NotFoundException("Item", itemId));
        return variantRepository.findByItemIdAndActiveTrueOrderByIdAsc(itemId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse get(Long id){
        return toResponse(variantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Variant", id)));
    }

    @Transactional
    public VariantResponse create(Long itemId, VariantCreateRequest request){
        Item item = itemRepository.findByIdAndActiveTrue(itemId)
                .orElseThrow(() -> new NotFoundException("Item", itemId));
        if(variantRepository.existsBySku(request.sku())){
            throw new ConflictException("Variant with SKU '" + request.sku() + "' already exists");
        }
        int initialStock = request.initialStock() == null ? 0 : request.initialStock();
        Variant variant = new Variant(item, request.sku(), request.name(), request.price(), initialStock);
        Variant saved = variantRepository.save(variant);
        stockLedgerService.recordInitial(saved, "Initial stock");
        return toResponse(saved);
    }

    @Transactional
    public VariantResponse update(Long id, VariantUpdateRequest request){
        Variant variant = variantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Variant", id));
        variant.updateDetails(request.name(), request.price());
        return toResponse(variant);
    }

    @Transactional
    public void delete(Long id){
        Variant variant = variantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Variant", id));
        variant.deactivate();
    }

    @Transactional
    public VariantResponse adjustStock(Long id, StockAdjustmentRequest request){
        if(request.quantityDelta() == 0){
            throw new BadRequestException("quantityDelta must not be zero");
        }
        variantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Variant", id));
        Variant updated = stockLedgerService
                .applyDelta(id, request.quantityDelta(), MovementType.ADJUSTMENT, null, request.reason())
                .orElseThrow(() -> new ConflictException(
                        "Adjustment of " + request.quantityDelta() + " would make stock negative"
                ));
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> movements(Long variantId, Pageable pageable){
        variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Variant", variantId));
        Page<StockMovement> page = stockMovementRepository.findByVariantIdOrderByIdDesc(variantId, pageable);
        return PageResponse.of(page.map(this::toMovementResponse));
    }

    private VariantResponse toResponse(Variant variant){
        return new VariantResponse(
                variant.getId(),
                variant.getItem().getId(),
                variant.getItem().getSku(),
                variant.getSku(),
                variant.getName(),
                variant.getPrice(),
                variant.effectivePrice(),
                variant.getStock(),
                variant.isActive(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }

    private StockMovementResponse toMovementResponse(StockMovement movement){
        return new StockMovementResponse(
                movement.getId(),
                movement.getVariant().getId(),
                movement.getType().name(),
                movement.getQuantityDelta(),
                movement.getStockAfter(),
                movement.getReferenceId(),
                movement.getReason(),
                movement.getCreatedAt()
        );
    }
}


