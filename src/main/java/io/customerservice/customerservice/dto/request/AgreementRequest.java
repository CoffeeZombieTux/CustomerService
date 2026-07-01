package io.customerservice.customerservice.dto.request;

import io.customerservice.customerservice.entity.AgreementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AgreementRequest(
        @NotNull AgreementType type,
        @NotNull @Min(1) Integer version,
        boolean accepted
) {}