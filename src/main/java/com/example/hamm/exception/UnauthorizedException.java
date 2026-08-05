package com.example.hamm.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.FORBIDDEN, "UNAUTHORIZED", message);
    }
}
