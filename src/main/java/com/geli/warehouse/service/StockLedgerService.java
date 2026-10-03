package com.geli.warehouse.service;

import com.geli.warehouse.model.MovementType;
import com.geli.warehouse.model.StockMovement;
import com.geli.warehouse.model.Variant;
import com.geli.warehouse.repository.StockMovementRepository;
import com.geli.warehouse.repository.VariantRepository;
import com.geli.warehouse.util.Timestamps;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StockLedgerService {
    private final VariantRepository variantRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockLedgerService(VariantRepository variantRepository, StockMovementRepository stockMovementRepository) {
        this.variantRepository = variantRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional
    public Optional<Variant> applyDelta(Long variantId, int delta, MovementType type, Long referenceId, String reason){
        if(delta == 0){
            throw new IllegalArgumentException("delta must not be zero");
        }
        if(variantRepository.applyStockDelta(variantId, delta, Timestamps.now()) == 0){
            return Optional.empty();
        }

        Variant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Variant " + variantId + " disappeared"));
        stockMovementRepository.save(new StockMovement(
                variant, type, delta, variant.getStock(), referenceId, reason
        ));
        return Optional.of(variant);
    }

    @Transactional
    public void recordInitial(Variant variant, String reason){
        if(variant.getStock() <= 0){
            return;
        }
        stockMovementRepository.save(new StockMovement(
                variant, MovementType.INITIAL, variant.getStock(), variant.getStock(), null, reason
        ));
    }
}
