package io.customerservice.customerservice.dto.request.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InternalBonusAccountChangeRequest(
        @NotNull @Min(1) Integer amount,
        @NotNull UUID idempotencyKey
) {}