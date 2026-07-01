package io.customerservice.customerservice.dto.request.internal;

import jakarta.validation.constraints.NotBlank;

public record InternalValidateTokenRequest(@NotBlank String token) {}
