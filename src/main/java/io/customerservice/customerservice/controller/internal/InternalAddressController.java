package io.customerservice.customerservice.controller.internal;

import io.customerservice.customerservice.dto.request.AddressRequest;
import io.customerservice.customerservice.dto.response.AddressResponse;
import io.customerservice.customerservice.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Internal — Addresses")
@SecurityRequirement(name = "internalApiKey")
@RestController
@RequestMapping("/api/v1/internal/customers/{customerId}/addresses")
@RequiredArgsConstructor
public class InternalAddressController {
    private final AddressService addressService;


    @Operation(summary = "List addresses")
    @GetMapping
    public List<AddressResponse> getAddresses(@PathVariable Long customerId) {
        return addressService.getAll(customerId);
    }

    @Operation(summary = "Add address")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@PathVariable Long customerId, @Valid @RequestBody AddressRequest request) {
        return addressService.create(request, customerId);
    }

    @Operation(summary = "Update address")
    @PutMapping("/{addressId}")
    public AddressResponse updateAddress(@PathVariable Long customerId, @PathVariable Long addressId,
                                         @Valid @RequestBody AddressRequest request) {
        return addressService.update(addressId, customerId, request);
    }

    @Operation(summary = "Delete address")
    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long customerId, @PathVariable Long addressId) {
        addressService.delete(addressId, customerId);
    }
}
