package com.geli.warehouse.repository;

import com.geli.warehouse.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Page<Sale> findAllByOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"lines", "lines.variant"})
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findDetailedById(@Param("id") Long id);
}
