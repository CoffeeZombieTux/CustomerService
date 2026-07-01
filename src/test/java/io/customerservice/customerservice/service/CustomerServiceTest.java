package io.customerservice.customerservice.service;

import io.customerservice.customerservice.dto.request.ChangePasswordRequest;
import io.customerservice.customerservice.dto.request.UpdateProfileRequest;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.dto.mapper.CustomerMapper;
import io.customerservice.customerservice.dto.mapper.SessionMapper;
import io.customerservice.customerservice.entity.Customer;
import io.customerservice.customerservice.entity.Role;
import io.customerservice.customerservice.exception.ResourceNotFoundException;
import io.customerservice.customerservice.repository.CustomerRepository;
import io.customerservice.customerservice.repository.RefreshTokenRepository;
import io.customerservice.customerservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthService authService;
    @Mock JwtService jwtService;
    @Mock CustomerMapper customerMapper;
    @Mock SessionMapper sessionMapper;

    CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, passwordEncoder, authService,
                jwtService, refreshTokenRepository, customerMapper, sessionMapper);
    }

    @Test
    void getProfile_success() {
        Customer customer = customer(1L);
        CustomerResponse expected = customerResponse(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expected);

        CustomerResponse result = customerService.getProfile(1L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getProfile_notFound_throws() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_success() {
        Customer customer = customer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("oldPass", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHashed");
        when(customerRepository.save(any())).thenReturn(customer);

        customerService.changePassword(new ChangePasswordRequest("oldPass", "newPass"), 1L);

        assertThat(customer.getPassword()).isEqualTo("newHashed");
        verify(authService).logoutAll(1L);
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        Customer customer = customer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest("wrong", "newPass");
        assertThatThrownBy(() -> customerService.changePassword(req, 1L))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void changePassword_sameAsOldPassword_throws() {
        Customer customer = customer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("samePass", "hashed")).thenReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest("samePass", "samePass");
        assertThatThrownBy(() -> customerService.changePassword(req, 1L))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void deleteProfile_success() {
        Customer customer = customer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.deleteProfile(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void updateProfile_success() {
        Customer customer = customer(1L);
        UpdateProfileRequest req = new UpdateProfileRequest("New", "Name", "+9999999999");
        CustomerResponse expected = customerResponse(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(expected);

        CustomerResponse result = customerService.updateProfile(1L, req);

        assertThat(result).isEqualTo(expected);
        verify(customerMapper).updateCustomer(req, customer);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Customer customer(Long id) {
        Customer c = new Customer();
        c.setRole(Role.CUSTOMER);
        c.setEmail("test@example.com");
        c.setPassword("hashed");
        c.setFirstName("Test");
        c.setLastName("User");
        c.setPhone("+1234567890");
        c.setEnabled(true);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static CustomerResponse customerResponse(Long id) {
        return new CustomerResponse(id, "test@example.com", "Test", "User", "+1234567890",
                Role.CUSTOMER, true, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
