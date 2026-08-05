package com.example.hamm.exception;

import org.springframework.http.HttpStatus;

public class DuplicateReferenceException extends ApiException {

    public DuplicateReferenceException(String reference) {
        super(HttpStatus.CONFLICT, "DUPLICATE_REFERENCE",
                "A transaction with reference '" + reference + "' already exists");
    }
}
