package com.geli.warehouse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geli.warehouse.exception.IdempotencyConflictException;
import com.geli.warehouse.model.IdempotencyKey;
import com.geli.warehouse.repository.IdempotencyKeyRepository;
import com.geli.warehouse.util.Timestamps;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    public record StoredResponse(int status, String body, Long resourceId){

    }

    public String fingerprint(Object payload){
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot fingerprint the request payload", ex);
        }
    }

    public Optional<StoredResponse> claim(String key, String fingerprint){
        if(idempotencyKeyRepository.claim(key, fingerprint, Timestamps.now().toEpochMilli()) == 1){
            return Optional.empty();
        }
        IdempotencyKey existing = idempotencyKeyRepository.findById(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency key vanished: " + key));
        if(!existing.matches(fingerprint)){
            throw IdempotencyConflictException.differentPayload();
        }

        if(!existing.isCompleted()){
            throw IdempotencyConflictException.stillInProgress();
        }

        return Optional.of(new StoredResponse(
                existing.getResponseStatus(), existing.getResponseBody(), existing.getResourceId()
        ));
    }

    public void complete(String key, int status, Object body, Long resourceId){
        idempotencyKeyRepository.complete(key, status, serialize(body), resourceId);
    }

    public <T> T parse(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored idempotent response is not readable", ex);
        }
    }

    private String serialize(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize the response", ex);
        }
    }


}
