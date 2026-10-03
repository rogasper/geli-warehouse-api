package com.geli.warehouse.repository;

import com.geli.warehouse.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    @Modifying
    @Query(value = """
        INSERT INTO idempotency_keys (key, request_fingerprint, created_at)
            VALUES (:key, :fingerprint, :createdAt)
            ON CONFLICT(key) DO NOTHING
    """, nativeQuery = true)
    int claim(@Param("key") String key,
              @Param("fingerprint") String fingerprint,
              @Param("createdAt") long createdAtMillis);

    @Modifying
    @Query("""
        UPDATE IdempotencyKey k
            SET k.responseStatus = :status,
                k.responseBody = :body,
                k.resourceId = :resourceId
            WHERE k.key = :key
    """)
    int complete(@Param("key") String key,
                 @Param("status") int status,
                 @Param("body") String body,
                 @Param("resourceId") Long resourceId);
}
