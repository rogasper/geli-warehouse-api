package com.geli.warehouse.service;

import com.geli.warehouse.dto.*;
import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.exception.ConflictException;
import com.geli.warehouse.exception.InsufficientStockException;
import com.geli.warehouse.exception.NotFoundException;
import com.geli.warehouse.model.MovementType;
import com.geli.warehouse.model.Sale;
import com.geli.warehouse.model.SaleLine;
import com.geli.warehouse.model.Variant;
import com.geli.warehouse.repository.SaleLineRepository;
import com.geli.warehouse.repository.SaleRepository;
import com.geli.warehouse.repository.VariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleLineRepository saleLineRepository;
    private final VariantRepository variantRepository;
    private final StockLedgerService stockLedgerService;
    private final IdempotencyService idempotencyService;

    public SaleService(SaleRepository saleRepository, SaleLineRepository saleLineRepository, VariantRepository variantRepository, StockLedgerService stockLedgerService, IdempotencyService idempotencyService) {
        this.saleRepository = saleRepository;
        this.saleLineRepository = saleLineRepository;
        this.variantRepository = variantRepository;
        this.stockLedgerService = stockLedgerService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public CreateResult create(SaleCreateRequest request, String idempotencyKey){
        if(idempotencyKey == null || idempotencyKey.isBlank()){
            return new CreateResult(201, doCreate(request), false);
        }

        String fingerprint = idempotencyService.fingerprint(request);
        Optional<IdempotencyService.StoredResponse> stored = idempotencyService.claim(idempotencyKey, fingerprint);

        if(stored.isPresent()) {
            IdempotencyService.StoredResponse replay = stored.get();
            return new CreateResult(replay.status(), idempotencyService.parse(replay.body(), SaleResponse.class), true);
        }
        SaleResponse response = doCreate(request);
        idempotencyService.complete(idempotencyKey, 201, response, response.id());
        return new CreateResult(201, response, false);
    }

    public record CreateResult(int status, SaleResponse sale, boolean replayed){}

    private SaleResponse doCreate(SaleCreateRequest request){
        List<SaleLineRequest> lines = request.lines().stream()
                .sorted(Comparator.comparing(SaleLineRequest::variantId))
                .toList();
        rejectDuplicateVariants(lines);
        Map<Long, Variant> variantsById = loadSellableVariants(lines);

        Sale sale = new Sale();
        for(SaleLineRequest line : lines){
            Variant variant = variantsById.get(line.variantId());
            sale.addLine(variant, line.quantity(), variant.effectivePrice());
        }

        Sale saved = saleRepository.saveAndFlush(sale);

        List<InsufficientStockException.Shortage> shortages = new ArrayList<>();
        for(SaleLineRequest line : lines){
            Variant variant = variantsById.get(line.variantId());
            boolean applied = stockLedgerService
                    .applyDelta(variant.getId(), -line.quantity(), MovementType.SALE, saved.getId(), null)
                    .isPresent();
            if(!applied){
                int available = variantRepository.findStockById(variant.getId()).orElse(0);
                shortages.add(new InsufficientStockException.Shortage(
                        variant.getId(), variant.getSku(), line.quantity(), available
                ));
            }
        }
        if(!shortages.isEmpty()){
            throw new InsufficientStockException(shortages);
        }
        return toResponse(saved, saved.getLines());
    }

    @Transactional(readOnly = true)
    public SaleResponse get(Long id){
        Sale sale = saleRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Sale", id));
        return toResponse(sale, sale.getLines());
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> list(Pageable pageable){
        Page<Sale> page = saleRepository.findAllByOrderByIdDesc(pageable);
        List<Long> saleIds = page.getContent().stream().map(Sale::getId).toList();
        Map<Long, List<SaleLine>> linesBySale = saleIds.isEmpty()
                ? Map.of()
                : saleLineRepository.findBySaleIdIn(saleIds).stream()
                .collect(Collectors.groupingBy(line -> line.getSale().getId()));
        return PageResponse.of(page.map(
                sale -> toResponse(sale, linesBySale.getOrDefault(sale.getId(), List.of()))
        ));
    }

    private void rejectDuplicateVariants(List<SaleLineRequest> lines){
        Set<Long> seen = new HashSet<>();
        List<ErrorResponse.ErrorDetail> duplicates = new ArrayList<>();
        for(SaleLineRequest line : lines){
            if(!seen.add(line.variantId())){
                duplicates.add(new ErrorResponse.ErrorDetail(
                        "variantId", "duplicate line for variant " + line.variantId()
                ));
            }
        }
        if(!duplicates.isEmpty()){
            throw new BadRequestException("A variant may appear only once per sale", duplicates);
        }
    }

    private Map<Long, Variant> loadSellableVariants(List<SaleLineRequest> lines){
        List<Long> variantIds = lines.stream().map(SaleLineRequest::variantId).toList();
        Map<Long, Variant> variantsById = variantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(Variant::getId, Function.identity()));
        for (Long variantId : variantIds){
            Variant variant = variantsById.get(variantId);
            if(variant == null){
                throw new NotFoundException("Variant", variantId);
            }
            if(!variant.isActive() || !variant.getItem().isActive()){
                throw new ConflictException("Variant " + variant.getSku() + " is not available for sale");
            }
        }
        return variantsById;
    }

    private SaleResponse toResponse(Sale sale, List<SaleLine> lines) {
        List<SaleLineResponse> lineResponses = lines.stream()
                .map(line -> new SaleLineResponse(
                        line.getVariant().getId(),
                        line.getVariant().getSku(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getLineTotal()))
                .toList();
        return new SaleResponse(sale.getId(), sale.getCreatedAt(), sale.getTotalAmount(), lineResponses);
    }
}
