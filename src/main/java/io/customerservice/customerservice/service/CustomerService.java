package io.customerservice.customerservice.service;

import io.customerservice.customerservice.dto.mapper.CustomerMapper;
import io.customerservice.customerservice.dto.mapper.SessionMapper;
import io.customerservice.customerservice.dto.request.ChangePasswordRequest;
import io.customerservice.customerservice.dto.request.UpdateProfileRequest;
import io.customerservice.customerservice.dto.request.internal.InternalValidateTokenRequest;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.dto.response.SessionResponse;
import io.customerservice.customerservice.entity.Customer;
import io.customerservice.customerservice.exception.InvalidTokenException;
import io.customerservice.customerservice.exception.ResourceNotFoundException;
import io.customerservice.customerservice.repository.CustomerRepository;
import io.customerservice.customerservice.repository.RefreshTokenRepository;
import io.customerservice.customerservice.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerMapper customerMapper;
    private final SessionMapper sessionMapper;

    public CustomerResponse getProfile(Long customerId) {
        return customerMapper.toResponse(customerRepository.findById(customerId).
                orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE)));
    }

    public Page<CustomerResponse> listCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(customerMapper::toResponse);
    }

    public List<SessionResponse> listSessions(Long customerId) {
        return refreshTokenRepository.findAllByCustomerIdAndExpiredAtAfter(customerId, OffsetDateTime.now())
                .stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    public CustomerResponse validateToken(InternalValidateTokenRequest request) {
        Claims claims;
        try {
            claims = jwtService.extractClaims(request.token());
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }
        String sessionId = claims.get("sessionId", String.class);
        if (sessionId == null || !refreshTokenRepository.existsBySessionId(parseSessionId(sessionId))) {
            throw new InvalidTokenException();
        }
        Long customerId = Long.valueOf(claims.getSubject());
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));
        if (!customer.isEnabled()) throw new InvalidTokenException();
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateProfile(Long customerId, UpdateProfileRequest updateProfileRequest) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));
        customerMapper.updateCustomer(updateProfileRequest, customer);
        customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public void deleteProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));
        customerRepository.delete(customer);
        log.info("customer.delete customerId={}", customerId);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest changePasswordRequest, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));

        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(), customer.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        if (changePasswordRequest.newPassword().equals(changePasswordRequest.currentPassword())) {
            throw new BadCredentialsException("New password cannot be the same as the current password");
        }
        customer.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        customerRepository.save(customer);
        authService.logoutAll(customer.getId());
        log.info("customer.password_change customerId={}", customerId);
    }

    private static UUID parseSessionId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }
}
