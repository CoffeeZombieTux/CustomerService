package io.customerservice.customerservice.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}