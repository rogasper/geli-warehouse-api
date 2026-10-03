package com.geli.warehouse.model;

import jakarta.persistence.*;

import com.geli.warehouse.util.Timestamps;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    @Column(name = "key", nullable = false, length = 255)
    private String key;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKey(){}

    public IdempotencyKey(String key, String requestFingerprint) {
        this.key = key;
        this.requestFingerprint = requestFingerprint;
    }

    @PrePersist
    void onCreate(){
        this.createdAt = Timestamps.now();
    }

    public void complete(int responseStatus, String responseBody, Long resourceId){
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.resourceId = resourceId;
    }

    public boolean isCompleted(){
        return responseStatus != null;
    }

    public boolean matches(String fingerprint){
        return requestFingerprint.equals(fingerprint);
    }

    public String getKey() {
        return key;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

