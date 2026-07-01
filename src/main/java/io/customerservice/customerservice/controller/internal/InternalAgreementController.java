package io.customerservice.customerservice.controller.internal;

import io.customerservice.customerservice.dto.request.AgreementRequest;
import io.customerservice.customerservice.dto.response.AgreementResponse;
import io.customerservice.customerservice.service.AgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Internal — Agreements")
@SecurityRequirement(name = "internalApiKey")
@RestController
@RequestMapping("/api/v1/internal/customers/{customerId}/agreements")
@RequiredArgsConstructor
public class InternalAgreementController {

    private final AgreementService agreementService;

    @Operation(summary = "Get full agreement history")
    @GetMapping
    public List<AgreementResponse> getAgreements(@PathVariable Long customerId) {
        return agreementService.getAgreements(customerId);
    }

    @Operation(summary = "Get latest accepted consent per type")
    @GetMapping("/active")
    public List<AgreementResponse> getActiveAgreements(@PathVariable Long customerId) {
        return agreementService.getActiveAgreements(customerId);
    }

    @Operation(summary = "Submit consent changes")
    @PostMapping
    public List<AgreementResponse> updateAgreement(@PathVariable Long customerId, @Valid @RequestBody AgreementRequest request) {
        return agreementService.updateAgreements(customerId, request);
    }
}
