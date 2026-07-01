package io.customerservice.customerservice.dto.response;

import io.customerservice.customerservice.entity.Role;

import java.time.OffsetDateTime;

public record CustomerResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}