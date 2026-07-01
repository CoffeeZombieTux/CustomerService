package io.customerservice.customerservice.dto.request;

import io.customerservice.customerservice.dto.validation.ValidPassword;
import io.customerservice.customerservice.dto.validation.ValidPhone;
import jakarta.validation.constraints.*;

import java.util.List;

public record RegisterRequest(
        @NotBlank @Size(max = 70) @Email String email,
        @ValidPassword String password,
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @ValidPhone String phone,
        @NotEmpty List<AgreementRequest> agreements
) {}