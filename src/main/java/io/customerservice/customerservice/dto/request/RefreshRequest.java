package io.customerservice.customerservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String token) {}
