package io.customerservice.customerservice.controller;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.dto.request.*;
import io.customerservice.customerservice.dto.response.AuthResponse;
import io.customerservice.customerservice.dto.response.MessageResponse;
import io.customerservice.customerservice.exception.InvalidTokenException;
import io.customerservice.customerservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AppProperties appProperties;

    @Operation(summary = "Validate activation token (step 1 of two-step activation)")
    @GetMapping("/activate")
    public ResponseEntity<Void> activate(@RequestParam UUID token) {
        authService.validateActivationToken(token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Activate account (step 2 of two-step activation)")
    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@RequestBody ActivationTokenRequest request) {
        try {
            authService.activate(request.token());
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(appProperties.activation().failUrl()))
                    .build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(appProperties.activation().successUrl()))
                .build();
    }

    @Operation(summary = "Resend activation email")
    @PostMapping("/resend-activation")
    public MessageResponse resendActivationEmail(@Valid @RequestBody ResendActivationRequest request) {
        authService.resendActivationEmail(request);
        return new MessageResponse(appProperties.messages().resendActivation());
    }

    @Operation(summary = "Register a new customer")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Log in and receive tokens")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "Log out (invalidate refresh token)")
    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }
}
