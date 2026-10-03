package com.geli.warehouse.repository;

import com.geli.warehouse.model.Item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsBySku(String sku);

    Optional<Item> findByIdAndActiveTrue(Long id);

    @Query("""
        SELECT i FROM Item i
        WHERE i.active = :active
        AND (:q is NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :q, '%')))
""")
    Page<Item> search(@Param("q") String q, @Param("active") boolean active, Pageable pageable);
}
