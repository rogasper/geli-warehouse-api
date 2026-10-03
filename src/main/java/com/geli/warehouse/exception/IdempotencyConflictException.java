package com.geli.warehouse.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends ApiException {
    public IdempotencyConflictException(String message) {
        super(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", message);
    }

    public static IdempotencyConflictException differentPayload(){
        return new IdempotencyConflictException(
                "This Idempotency-Key was already used with a different request payload"
        );
    }

    public static IdempotencyConflictException stillInProgress(){
        return new IdempotencyConflictException(
                "A request with this Idempotency-Key is still being processed"
        );
    }
}
