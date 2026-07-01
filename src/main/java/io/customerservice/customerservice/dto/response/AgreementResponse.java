package io.customerservice.customerservice.dto.response;

import io.customerservice.customerservice.entity.AgreementType;

import java.time.OffsetDateTime;

public record AgreementResponse(AgreementType type,
                                Integer version,
                                boolean accepted,
                                OffsetDateTime createdAt,
                                OffsetDateTime updatedAt
) {}

