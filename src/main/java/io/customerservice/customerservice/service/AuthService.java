package io.customerservice.customerservice.service;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.dto.request.*;
import io.customerservice.customerservice.dto.response.AuthResponse;
import io.customerservice.customerservice.dto.response.MessageResponse;
import io.customerservice.customerservice.entity.*;
import io.customerservice.customerservice.exception.*;
import io.customerservice.customerservice.repository.*;
import io.customerservice.customerservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ActivationTokenRepository activationTokenRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final CustomerAgreementRepository customerAgreementRepository;
    private final BonusAccountRepository bonusAccountRepository;

    @Transactional
    public MessageResponse register(@NonNull RegisterRequest registerRequest) {
        boolean exists = customerRepository.existsByEmailIgnoreCase(registerRequest.email());
        if (exists) {
            throw new EmailAlreadyExistsException();
        }

        Customer customer = new Customer();
        customer.setFirstName(registerRequest.firstName());
        customer.setLastName(registerRequest.lastName());
        customer.setEmail(registerRequest.email());
        customer.setPhone(registerRequest.phone());
        customer.setPassword(passwordEncoder.encode(registerRequest.password()));
        customer.setRole(Role.CUSTOMER);
        customer.setEnabled(false);

        customerRepository.save(customer);

        BonusAccount bonusAccount = new BonusAccount();
        bonusAccount.setCustomer(customer);
        bonusAccountRepository.save(bonusAccount);

        Set<AgreementType> acceptedTypes = registerRequest.agreements().stream()
                .filter(AgreementRequest::accepted)
                .map(AgreementRequest::type)
                .collect(Collectors.toSet());
        if (!acceptedTypes.containsAll(appProperties.agreements().mandatory())) {
            throw new MandatoryAgreementException();
        }

        addCustomerAgreements(customer, registerRequest.agreements());

        ActivationToken activationToken = new ActivationToken(appProperties.activation().tokenLivenessHours());
        activationToken.setCustomer(customer);
        activationTokenRepository.save(activationToken);

        emailService.sendActivationEmail(customer.getEmail(), getActivationLink(activationToken));

        log.info("auth.register success customerId={}", customer.getId());
        return new MessageResponse(appProperties.messages().registrationSuccess());
    }

    public void validateActivationToken(UUID token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(InvalidTokenException::new);
        if (activationToken.getExpiredAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException();
        }
    }

    @Transactional
    public void activate(UUID token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(InvalidTokenException::new);
        if (activationToken.getExpiredAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException();
        }
        Customer customer = activationToken.getCustomer();
        customer.setEnabled(true);
        customerRepository.save(customer);
        activationTokenRepository.delete(activationToken);
        log.info("auth.activate success customerId={}", customer.getId());
    }

    @Transactional
    public void resendActivationEmail(ResendActivationRequest request) {
        Optional<Customer> customerOpt = customerRepository.findByEmailIgnoreCase(request.email());
        if (customerOpt.isEmpty()) {
            return;
        }
        Customer customer = customerOpt.get();
        ActivationToken activationToken = new ActivationToken(appProperties.activation().tokenLivenessHours());
        activationToken.setCustomer(customer);
        activationTokenRepository.save(activationToken);
        emailService.sendActivationEmail(customer.getEmail(), getActivationLink(activationToken));
    }

    @Transactional
    public AuthResponse login(@NonNull LoginRequest loginRequest) {
        Customer customer = customerRepository.findByEmailIgnoreCase(loginRequest.email()).orElse(null);
        if (customer == null) {
            log.warn("auth.login failed reason=unknown_email");
            throw new BadCredentialsException("");
        }
        if (!passwordEncoder.matches(loginRequest.password(), customer.getPassword())) {
            log.warn("auth.login failed reason=wrong_password customerId={}", customer.getId());
            throw new BadCredentialsException("");
        }
        if (!customer.isEnabled()) {
            log.warn("auth.login failed reason=account_disabled customerId={}", customer.getId());
            throw new BadCredentialsException("");
        }
        return createSession(customer);
    }

    @Transactional
    public @NonNull AuthResponse refresh(RefreshRequest request) {
        RefreshToken oldRefreshToken = refreshTokenRepository.findByTokenHash(sha256Hex(request.token())).orElse(null);
        if (oldRefreshToken == null) {
            log.warn("auth.token.refresh failed reason=unknown_token");
            throw new InvalidTokenException();
        }
        if (oldRefreshToken.getExpiredAt().isBefore(OffsetDateTime.now())) {
            log.warn("auth.token.refresh failed reason=expired_token customerId={}", oldRefreshToken.getCustomer().getId());
            throw new InvalidTokenException();
        }
        Customer customer = oldRefreshToken.getCustomer();
        refreshTokenRepository.delete(oldRefreshToken);
        if (!customer.isEnabled()) {
            log.warn("auth.token.refresh failed reason=account_disabled customerId={}", customer.getId());
            throw new BadCredentialsException("");
        }
        return createSession(customer);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(sha256Hex(request.token())).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void logoutAll(Long customerId) {
        refreshTokenRepository.deleteByCustomerId(customerId);
    }

    private @NonNull AuthResponse createSession(@NonNull Customer customer) {
        Customer locked = customerRepository.findByIdForUpdate(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));
        if (refreshTokenRepository.countByCustomerId(locked.getId()) >= appProperties.security().maxSessions()) {
            throw new TooManySessionsException();
        }

        String rawRefreshToken = generateRefreshSecret();
        RefreshToken session = new RefreshToken(appProperties.security().refreshTokenLivenessDays(), sha256Hex(rawRefreshToken));
        session.setCustomer(locked);
        refreshTokenRepository.save(session);

        String accessToken = jwtService.generateCustomerToken(locked, session.getSessionId());
        return new AuthResponse(accessToken, rawRefreshToken);
    }

    private @NonNull String getActivationLink(@NonNull ActivationToken activationToken) {
        return appProperties.activation().buttonUrl() + "?token=" + activationToken.getToken();
    }

    private void addCustomerAgreements(Customer customer, @NonNull List<AgreementRequest> agreements) {
        agreements.forEach(agreement -> {
            CustomerAgreement customerAgreement = new CustomerAgreement();
            customerAgreement.setAgreementType(agreement.type());
            customerAgreement.setVersion(agreement.version());
            customerAgreement.setAccepted(agreement.accepted());
            customerAgreement.setCustomer(customer);
            customerAgreementRepository.save(customerAgreement);
        });
    }

    private static String generateRefreshSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
