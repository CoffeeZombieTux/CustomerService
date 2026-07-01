package io.customerservice.customerservice.service;

import io.customerservice.customerservice.dto.mapper.CustomerMapper;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.entity.Customer;
import io.customerservice.customerservice.entity.RefreshToken;
import io.customerservice.customerservice.exception.ResourceNotFoundException;
import io.customerservice.customerservice.repository.CustomerRepository;
import io.customerservice.customerservice.repository.RefreshTokenRepository;
import io.customerservice.customerservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerMapper customerMapper;
    private final SecurityUtils securityUtils;

    @Transactional
    public CustomerResponse setEnabled(Long customerId, boolean enabled) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));
        customer.setEnabled(enabled);
        if (!enabled) {
            deleteAllSessions(customerId);
        }
        customerRepository.save(customer);
        log.info("admin.customer.{} targetCustomerId={} by={}", enabled ? "enable" : "disable",
                customerId, securityUtils.currentCustomerId());
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public void terminateAllSessions(Long customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));
        deleteAllSessions(customerId);
        log.info("admin.session.terminate.all targetCustomerId={} by={}", customerId, securityUtils.currentCustomerId());
    }

    @Transactional
    public void terminateSession(Long customerId, Long sessionId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));
        RefreshToken session = refreshTokenRepository.findByIdAndCustomerId(sessionId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(RefreshToken.RESOURCE));
        refreshTokenRepository.delete(session);
        log.info("admin.session.terminate sessionId={} targetCustomerId={} by={}", sessionId, customerId,
                securityUtils.currentCustomerId());
    }

    private void deleteAllSessions(Long customerId) {
        refreshTokenRepository.deleteByCustomerId(customerId);
    }
}
