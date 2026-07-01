package io.customerservice.customerservice.dto.request;

import io.customerservice.customerservice.dto.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @ValidPassword String newPassword
) {}