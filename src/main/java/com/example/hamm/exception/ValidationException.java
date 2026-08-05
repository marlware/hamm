package com.example.hamm.exception;

import org.springframework.http.HttpStatus;

/**
 * Business-rule validation failure (as opposed to bean-validation constraint
 * violations, which are handled separately).
 */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message);
    }
}
