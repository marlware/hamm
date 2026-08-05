package com.example.hamm.exception;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    @Builder
    public record FieldError(String field, String message) {
    }
}
