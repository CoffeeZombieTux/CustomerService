package io.customerservice.customerservice.controller;

import io.customerservice.customerservice.dto.request.ChangePasswordRequest;
import io.customerservice.customerservice.dto.request.UpdateProfileRequest;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.security.SecurityUtils;
import io.customerservice.customerservice.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customer Profile")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get own profile")
    @GetMapping("/me")
    public CustomerResponse getProfile() {
        Long customerId = securityUtils.currentCustomerId();
        return customerService.getProfile(customerId);
    }

    @Operation(summary = "Change password (invalidates all sessions)")
    @PostMapping("/me/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        Long customerId = securityUtils.currentCustomerId();
        customerService.changePassword(changePasswordRequest, customerId);
    }

    @Operation(summary = "Update profile (null fields are ignored)")
    @PatchMapping("/me")
    public CustomerResponse updateProfile(@Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        Long customerId = securityUtils.currentCustomerId();
        return customerService.updateProfile(customerId, updateProfileRequest);
    }

    @Operation(summary = "Delete own account")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile() {
        Long customerId = securityUtils.currentCustomerId();
        customerService.deleteProfile(customerId);
    }
}
