package io.customerservice.customerservice.controller;

import io.customerservice.customerservice.dto.request.AgreementRequest;
import io.customerservice.customerservice.dto.response.AgreementResponse;
import io.customerservice.customerservice.security.SecurityUtils;
import io.customerservice.customerservice.service.AgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Agreements")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/customers/me/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get full agreement history")
    @GetMapping
    public List<AgreementResponse> getAgreements() {
        Long customerId = securityUtils.currentCustomerId();
        return agreementService.getAgreements(customerId);
    }

    @Operation(summary = "Get latest accepted consent per type")
    @GetMapping("/active")
    public List<AgreementResponse> getActiveAgreements() {
        Long customerId = securityUtils.currentCustomerId();
        return agreementService.getActiveAgreements(customerId);
    }

    @Operation(summary = "Submit consent changes")
    @PostMapping
    public List<AgreementResponse> updateAgreements(@Valid @RequestBody AgreementRequest request) {
        Long customerId = securityUtils.currentCustomerId();
        return agreementService.updateAgreements(customerId, request);
    }
}
