package io.customerservice.customerservice.dto.response;

import io.customerservice.customerservice.entity.AddressType;

import java.time.OffsetDateTime;

public record AddressResponse(
        Long id,
        AddressType type,
        String street,
        String city,
        String postalCode,
        String country,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
