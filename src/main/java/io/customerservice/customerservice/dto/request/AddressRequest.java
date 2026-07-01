package io.customerservice.customerservice.dto.request;

import io.customerservice.customerservice.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotNull AddressType type,
        @NotBlank @Size(max=255) String street,
        @NotBlank @Size(max=40) String city,
        @NotBlank @Size(max=10) String postalCode,
        @NotBlank @Size(min=2, max=2) @Pattern(
                regexp = "^[A-Z]{2}$",
                message = "must be a valid ISO 3166-1 alpha-2 country code"
        ) String country
) {}
