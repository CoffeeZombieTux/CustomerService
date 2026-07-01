package io.customerservice.customerservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(@NonNull Exception ex, @NonNull HttpServletRequest request) {
        log.error("Unhandled exception {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "internal-error", request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailConflict(@NonNull EmailAlreadyExistsException ex,
                                             @NonNull HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "email-conflict", request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(@NonNull InvalidTokenException ex, @NonNull HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), "invalid-token", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(@NonNull ResourceNotFoundException ex, @NonNull HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found", request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(@NonNull InsufficientBalanceException ex,
                                                   @NonNull HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), "insufficient-balance", request);
    }

    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public ProblemDetail handleDuplicateIdempotencyKey(@NonNull DuplicateIdempotencyKeyException ex,
                                                       @NonNull HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "duplicate-idempotency-key", request);
    }

    @ExceptionHandler(TooManySessionsException.class)
    public ProblemDetail handleTooManySessions(@NonNull TooManySessionsException ex,
                                               @NonNull HttpServletRequest request) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), "too-many-sessions", request);
    }

    @ExceptionHandler(MandatoryAgreementException.class)
    public ProblemDetail handleMandatoryAgreement(@NonNull MandatoryAgreementException ex,
                                                  @NonNull HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), "mandatory-agreement-required", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(@NonNull BadCredentialsException ex,
                                              @NonNull HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", "invalid-credentials", request);
    }

    // Method-security denials (@PreAuthorize) throw AuthorizationDeniedException at controller
    // invocation, so they surface here rather than in the filter chain's CustomAccessDeniedHandler.
    // Body is kept identical to that handler for a consistent 403 across both paths.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(@NonNull AccessDeniedException ex,
                                            @NonNull HttpServletRequest request) {
        log.warn("security.access_denied {} {}", request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.FORBIDDEN, "Access denied", "forbidden", request);
    }

    @ExceptionHandler(DuplicateAgreementException.class)
    public ProblemDetail handleDuplicateAgreementException(@NonNull DuplicateAgreementException ex,
                                                           @NonNull HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "duplicate-customer-agreement", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(@NonNull MethodArgumentNotValidException ex,
                                          @NonNull HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return problem(HttpStatus.BAD_REQUEST, detail, "validation-error", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Resource already exists", "conflict", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(@NonNull MethodArgumentTypeMismatchException ex,
                                            @NonNull HttpServletRequest request) {
        String detail = "Parameter '" + ex.getName() + "' has invalid value";
        return problem(HttpStatus.BAD_REQUEST, detail, "type-mismatch", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(@NonNull HttpMessageNotReadableException ex,
                                          @NonNull HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body", "malformed-request", request);
    }

    private ProblemDetail problem(HttpStatus status, String detail, String type,
                                  HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("/problems/" + type));
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }
}