package io.customerservice.customerservice.controller.internal;

import io.customerservice.customerservice.dto.request.internal.InternalBonusAccountChangeRequest;
import io.customerservice.customerservice.dto.response.BonusAccountResponse;
import io.customerservice.customerservice.service.BonusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Internal — Bonus")
@SecurityRequirement(name = "internalApiKey")
@RestController
@RequestMapping("/api/v1/internal/customers/{customerId}/bonus")
@RequiredArgsConstructor
public class InternalBonusController {

    private final BonusService bonusService;

    @Operation(summary = "Get bonus balance")
    @GetMapping
    public BonusAccountResponse getBalance(@PathVariable Long customerId) {
        return bonusService.getBalance(customerId);
    }

    @Operation(summary = "Credit bonus points (idempotent)")
    @PostMapping("/credit")
    public BonusAccountResponse credit(@PathVariable Long customerId, @Valid @RequestBody InternalBonusAccountChangeRequest request) {
        return bonusService.credit(customerId, request);
    }

    @Operation(summary = "Redeem bonus points")
    @PostMapping("/redeem")
    public BonusAccountResponse redeem(@PathVariable Long customerId, @Valid @RequestBody InternalBonusAccountChangeRequest request) {
        return bonusService.redeem(customerId, request);
    }
}