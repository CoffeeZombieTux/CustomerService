package io.customerservice.customerservice.controller;

import io.customerservice.customerservice.dto.response.BonusAccountResponse;
import io.customerservice.customerservice.security.SecurityUtils;
import io.customerservice.customerservice.service.BonusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bonus")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/bonus")
@RequiredArgsConstructor
public class BonusController {

    private final BonusService bonusService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get own bonus balance")
    @GetMapping
    public BonusAccountResponse getBalance() {
        Long customerId = securityUtils.currentCustomerId();
        return bonusService.getBalance(customerId);
    }
}