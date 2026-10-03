package com.geli.warehouse.dto;

import java.time.Instant;
import java.util.List;

/** Envelope used by every error response: {@code {"error": {...}}}. */
public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(
            String code,
            String message,
            List<ErrorDetail> details,
            Instant timestamp,
            String path) {
    }

    /** {@code field} is the offending request field (may be null). */
    public record ErrorDetail(String field, String issue) {
    }

    public static ErrorResponse of(String code, String message, List<ErrorDetail> details, String path) {
        return new ErrorResponse(new ErrorBody(code, message, details, Instant.now(), path));
    }

    public static ErrorResponse of(String code, String message, String path) {
        return of(code, message, List.of(), path);
    }
}
