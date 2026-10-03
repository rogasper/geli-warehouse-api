package com.geli.warehouse.exception;

import com.geli.warehouse.dto.ErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public class InsufficientStockException extends ApiException {
    public record Shortage(Long variantId, String variantSku, int requested, int available) {
    }

    public InsufficientStockException(List<Shortage> shortages) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                buildMessage(shortages), buildDetails(shortages));
    }

    private static String buildMessage(List<Shortage> shortages) {
        return "Insufficient stock for " + shortages.size()
                + (shortages.size() == 1 ? " line" : " lines");
    }

    private static List<ErrorResponse.ErrorDetail> buildDetails(List<Shortage> shortages) {
        return shortages.stream()
                .map(s -> new ErrorResponse.ErrorDetail(
                        "variant " + s.variantSku(),
                        "requested " + s.requested() + " but only " + s.available() + " available"))
                .toList();
    }
}
