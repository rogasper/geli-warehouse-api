package com.geli.warehouse.repository;

import com.geli.warehouse.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    
}
