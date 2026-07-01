package io.customerservice.customerservice.service;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.dto.request.*;
import io.customerservice.customerservice.dto.response.AuthResponse;
import io.customerservice.customerservice.dto.response.MessageResponse;
import io.customerservice.customerservice.entity.*;
import io.customerservice.customerservice.exception.EmailAlreadyExistsException;
import io.customerservice.customerservice.exception.InvalidTokenException;
import io.customerservice.customerservice.exception.MandatoryAgreementException;
import io.customerservice.customerservice.exception.TooManySessionsException;
import io.customerservice.customerservice.repository.*;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock ActivationTokenRepository activationTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;
    @Mock CustomerAgreementRepository customerAgreementRepository;
    @Mock BonusAccountRepository bonusAccountRepository;

    AuthService authService;

    private static final AppProperties APP_PROPERTIES = new AppProperties(
            new AppProperties.Jwt("dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHk=", 900000L),
            new AppProperties.Activation("http://localhost/activate", "http://localhost/success", "http://localhost/fail", 24),
            new AppProperties.Security(5, 7, 24),
            new AppProperties.Agreements(List.of(AgreementType.TERMS_OF_SERVICE, AgreementType.PRIVACY_POLICY, AgreementType.LOYALTY_PROGRAM_TERMS)),
            new AppProperties.Messages("Registration successful", "Activation link sent"),
            new AppProperties.Cleanup(new AppProperties.Cleanup.RefreshToken(1000)),
            new AppProperties.Internal("test-api-key")
    );

    private static final List<AgreementRequest> MANDATORY_AGREEMENTS = List.of(
            new AgreementRequest(AgreementType.TERMS_OF_SERVICE, 1, true),
            new AgreementRequest(AgreementType.PRIVACY_POLICY, 1, true),
            new AgreementRequest(AgreementType.LOYALTY_PROGRAM_TERMS, 1, true)
    );

    @BeforeEach
    void setUp() {
        authService = new AuthService(customerRepository, refreshTokenRepository, jwtService,
                passwordEncoder, activationTokenRepository, emailService, APP_PROPERTIES,
                customerAgreementRepository, bonusAccountRepository);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest(
                "new@example.com", "Password1!", "Test", "User", "+1234567890", MANDATORY_AGREEMENTS);
        when(customerRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.register(req);

        assertThat(response.message()).isEqualTo("Registration successful");
        verify(emailService).sendActivationEmail(eq("new@example.com"), anyString());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest(
                "taken@example.com", "Password1!", "Test", "User", "+1234567890", MANDATORY_AGREEMENTS);
        when(customerRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void register_missingMandatoryAgreement_throws() {
        List<AgreementRequest> incomplete = List.of(
                new AgreementRequest(AgreementType.TERMS_OF_SERVICE, 1, true)
        );
        RegisterRequest req = new RegisterRequest(
                "new@example.com", "Password1!", "Test", "User", "+1234567890", incomplete);
        when(customerRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(MandatoryAgreementException.class);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        Customer customer = enabledCustomer(1L);
        when(customerRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("Password1!", "hashed")).thenReturn(true);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(refreshTokenRepository.countByCustomerId(1L)).thenReturn(0);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateCustomerToken(any(), any())).thenReturn("access-token");

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "Password1!"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotNull();
    }

    @Test
    void login_unknownEmail_throws() {
        when(customerRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());
        LoginRequest req = new LoginRequest("unknown@example.com", "pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throws() {
        Customer customer = enabledCustomer(1L);
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        LoginRequest req = new LoginRequest("test@example.com", "wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_disabledAccount_throws() {
        Customer customer = disabledCustomer(1L);
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        LoginRequest req = new LoginRequest("test@example.com", "pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_sessionLimitReached_throws() {
        Customer customer = enabledCustomer(1L);
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(refreshTokenRepository.countByCustomerId(1L)).thenReturn(5); // maxSessions = 5
        LoginRequest req = new LoginRequest("test@example.com", "pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(TooManySessionsException.class);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_success() {
        Customer customer = enabledCustomer(1L);
        RefreshToken token = validRefreshToken(customer);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(refreshTokenRepository.countByCustomerId(1L)).thenReturn(0);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateCustomerToken(any(), any())).thenReturn("new-access-token");

        AuthResponse response = authService.refresh(new RefreshRequest("some-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void refresh_unknownToken_throws() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        RefreshRequest req = new RefreshRequest("unknown-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_expiredToken_throws() {
        Customer customer = enabledCustomer(1L);
        RefreshToken token = expiredRefreshToken(customer);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        RefreshRequest req = new RefreshRequest("expired-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_deletesToken() {
        Customer customer = enabledCustomer(1L);
        RefreshToken token = validRefreshToken(customer);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.logout(new RefreshRequest("some-token"));

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void logout_unknownToken_silentlyIgnored() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        authService.logout(new RefreshRequest("unknown-token"));

        verify(refreshTokenRepository, never()).delete(any());
    }

    // ── activate ──────────────────────────────────────────────────────────────

    @Test
    void activate_success() {
        Customer customer = disabledCustomer(1L);
        ActivationToken token = validActivationToken(customer);
        UUID tokenUuid = token.getToken();
        when(activationTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(token));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.activate(tokenUuid);

        assertThat(customer.isEnabled()).isTrue();
        verify(activationTokenRepository).delete(token);
    }

    @Test
    void activate_expiredToken_throws() {
        Customer customer = disabledCustomer(1L);
        ActivationToken token = expiredActivationToken(customer);
        UUID tokenUuid = token.getToken();
        when(activationTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.activate(tokenUuid))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ── resendActivationEmail ─────────────────────────────────────────────────

    @Test
    void resendActivationEmail_unknownEmail_silentlyIgnored() {
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        authService.resendActivationEmail(new ResendActivationRequest("unknown@example.com"));

        verify(emailService, never()).sendActivationEmail(anyString(), anyString());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Customer enabledCustomer(Long id) {
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

    private static Customer disabledCustomer(Long id) {
        Customer c = new Customer();
        c.setRole(Role.CUSTOMER);
        c.setEmail("test@example.com");
        c.setPassword("hashed");
        c.setFirstName("Test");
        c.setLastName("User");
        c.setPhone("+1234567890");
        c.setEnabled(false);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static RefreshToken validRefreshToken(Customer customer) {
        RefreshToken token = new RefreshToken(7, "hash");
        token.setCustomer(customer);
        return token;
    }

    private static RefreshToken expiredRefreshToken(Customer customer) {
        RefreshToken token = new RefreshToken(7, "hash");
        ReflectionTestUtils.setField(token, "expiredAt", OffsetDateTime.now().minusDays(1));
        token.setCustomer(customer);
        return token;
    }

    private static ActivationToken validActivationToken(Customer customer) {
        ActivationToken token = new ActivationToken(24);
        token.setCustomer(customer);
        return token;
    }

    private static ActivationToken expiredActivationToken(Customer customer) {
        ActivationToken token = new ActivationToken(24);
        ReflectionTestUtils.setField(token, "expiredAt", OffsetDateTime.now().minusHours(1));
        token.setCustomer(customer);
        return token;
    }
}
