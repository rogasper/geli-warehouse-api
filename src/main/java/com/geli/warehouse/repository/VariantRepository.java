package com.geli.warehouse.repository;

import com.geli.warehouse.model.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, Long> {
    boolean existsBySku(String sku);
    Optional<Variant> findByIdAndActiveTrue(Long id);

    List<Variant> findByItemIdAndActiveTrueOrderByIdAsc(Long itemId);

    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query("""
           UPDATE Variant v
                      SET v.stock = v.stock + :delta,
                          v.updatedAt = :now
                      WHERE v.id = :id
                        AND v.stock + :delta >= 0
           """)
    int applyStockDelta(@Param("id") Long id, @Param("delta") int delta, @Param("now") Instant now);


    @Query("""
        SELECT v.item.id AS itemId,
            COUNT(v) as variantCount,
            COALESCE(SUM(v.stock), 0) AS totalStock
        FROM Variant v
        WHERE v.item.id IN :itemIds
        AND v.active = true
        GROUP BY v.item.id            
    """)
    List<VariantSummary> summarizeByItemIds(@Param("itemIds")Collection<Long> itemIds);

    interface VariantSummary{
        Long getItemId();
        long getVariantCount();
        long getTotalStock();
    }
}
