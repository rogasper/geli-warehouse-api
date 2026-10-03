package com.geli.warehouse.exception;

import com.geli.warehouse.dto.ErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public class BadRequestException extends ApiException {
    public BadRequestException(String message){
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public BadRequestException(String message, List<ErrorResponse.ErrorDetail> details){
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, details);
    }
}
