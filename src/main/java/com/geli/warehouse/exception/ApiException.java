package com.geli.warehouse.exception;

import com.geli.warehouse.dto.ErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<ErrorResponse.ErrorDetail> details;

    protected ApiException(HttpStatus status, String code, String message){
        this(status, code, message, List.of());
    }

    protected ApiException(HttpStatus status, String code, String message, List<ErrorResponse.ErrorDetail> details){
        super(message);
        this.status = status;
        this.code = code;
        this.details = List.copyOf(details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<ErrorResponse.ErrorDetail> getDetails() {
        return details;
    }
}
