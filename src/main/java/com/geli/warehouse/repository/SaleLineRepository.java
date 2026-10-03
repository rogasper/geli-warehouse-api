package com.geli.warehouse.repository;

import com.geli.warehouse.model.SaleLine;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SaleLineRepository extends JpaRepository<SaleLine, Long> {
    @EntityGraph(attributePaths = "variant")
    List<SaleLine> findBySaleIdIn(Collection<Long> saleIds);
}
