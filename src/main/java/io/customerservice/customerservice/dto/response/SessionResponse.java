package io.customerservice.customerservice.dto.response;

import java.time.OffsetDateTime;

public record SessionResponse(
        Long id,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
