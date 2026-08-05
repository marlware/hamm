package com.example.hamm.auth;

public record AuthResponse(String accessToken, String tokenType, long expiresInMs) {

    public static AuthResponse of(String accessToken, long expiresInMs) {
        return new AuthResponse(accessToken, "Bearer", expiresInMs);
    }
}
