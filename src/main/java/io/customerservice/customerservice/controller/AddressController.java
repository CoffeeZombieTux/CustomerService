package io.customerservice.customerservice.controller;

import io.customerservice.customerservice.dto.request.AddressRequest;
import io.customerservice.customerservice.dto.response.AddressResponse;
import io.customerservice.customerservice.security.SecurityUtils;
import io.customerservice.customerservice.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Addresses")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "List own addresses")
    @GetMapping
    public List<AddressResponse> getAddresses() {
        Long customerId = securityUtils.currentCustomerId();
        return addressService.getAll(customerId);
    }

    @Operation(summary = "Add address")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@Valid @RequestBody AddressRequest request) {
        Long customerId = securityUtils.currentCustomerId();
        return addressService.create(request, customerId);
    }

    @Operation(summary = "Update address")
    @PutMapping("/{addressId}")
    public AddressResponse updateAddress(@PathVariable Long addressId, @Valid @RequestBody AddressRequest request) {
        Long customerId = securityUtils.currentCustomerId();
        return addressService.update(addressId, customerId, request);
    }

    @Operation(summary = "Delete address")
    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long addressId) {
        Long customerId = securityUtils.currentCustomerId();
        addressService.delete(addressId, customerId);
    }
}
