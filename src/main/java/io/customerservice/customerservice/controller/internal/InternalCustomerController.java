package io.customerservice.customerservice.controller.internal;

import io.customerservice.customerservice.dto.request.UpdateProfileRequest;
import io.customerservice.customerservice.dto.request.internal.InternalValidateTokenRequest;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.dto.response.SessionResponse;
import io.customerservice.customerservice.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Internal — Customers")
@SecurityRequirement(name = "internalApiKey")
@RestController
@RequestMapping("/api/v1/internal/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerService customerService;

    @Operation(summary = "List all customers (paginated)")
    @GetMapping
    public Page<CustomerResponse> listCustomers(@PageableDefault(size = 20) Pageable pageable) {
        return customerService.listCustomers(pageable);
    }

    @Operation(summary = "List active sessions for a customer")
    @GetMapping("/{customerId}/sessions")
    public List<SessionResponse> listSessions(@PathVariable Long customerId) {
        return customerService.listSessions(customerId);
    }

    @Operation(summary = "Get customer by ID")
    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable Long customerId) {
        return customerService.getProfile(customerId);
    }

    @Operation(summary = "Validate JWT and return customer data")
    @PostMapping("/validate")
    public CustomerResponse validateToken(@Valid @RequestBody InternalValidateTokenRequest request) {
        return customerService.validateToken(request);
    }

    @Operation(summary = "Update customer profile")
    @PatchMapping("/{customerId}")
    public CustomerResponse updateCustomer(@PathVariable Long customerId,
                                           @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        return customerService.updateProfile(customerId, updateProfileRequest);
    }
}