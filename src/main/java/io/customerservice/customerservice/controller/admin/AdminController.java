package io.customerservice.customerservice.controller.admin;

import io.customerservice.customerservice.dto.request.SetEnabledRequest;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.dto.response.SessionResponse;
import io.customerservice.customerservice.service.AdminService;
import io.customerservice.customerservice.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CustomerService customerService;

    @Operation(summary = "Enable or disable a customer account")
    @PatchMapping("/{customerId}/enabled")
    public CustomerResponse setEnabled(@PathVariable Long customerId,
                                       @Valid @RequestBody SetEnabledRequest setEnabledRequest) {
        return adminService.setEnabled(customerId, setEnabledRequest.enabled());
    }

    @Operation(summary = "List active sessions for a customer")
    @GetMapping("/{customerId}/sessions")
    public List<SessionResponse> listSessions(@PathVariable Long customerId) {
        return customerService.listSessions(customerId);
    }

    @Operation(summary = "Terminate all sessions for a customer")
    @DeleteMapping("/{customerId}/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminateAllSessions(@PathVariable Long customerId) {
        adminService.terminateAllSessions(customerId);
    }

    @Operation(summary = "Terminate a specific session")
    @DeleteMapping("/{customerId}/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminateSession(@PathVariable Long customerId, @PathVariable Long sessionId) {
        adminService.terminateSession(customerId, sessionId);
    }

    @Operation(summary = "Delete a customer account")
    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable Long customerId) {
        customerService.deleteProfile(customerId);
    }
}
