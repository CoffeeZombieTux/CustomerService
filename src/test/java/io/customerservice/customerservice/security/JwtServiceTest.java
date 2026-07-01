package io.customerservice.customerservice.security;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.entity.Customer;
import io.customerservice.customerservice.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 32-byte key required for HMAC-SHA256: base64("test-secret-key-for-unit-tests-only")
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHk=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt(TEST_SECRET, 900000L),
                new AppProperties.Activation("http://localhost/activate", "http://localhost/success", "http://localhost/fail", 24),
                new AppProperties.Security(5, 7, 24),
                new AppProperties.Agreements(List.of()),
                new AppProperties.Messages("", ""),
                new AppProperties.Cleanup(new AppProperties.Cleanup.RefreshToken(1000)),
                new AppProperties.Internal("key")
        );
        jwtService = new JwtService(props.jwt());
    }

    @Test
    void generateToken_claimsAreExtractable() {
        Customer customer = customer(42L, "user@example.com", Role.CUSTOMER);
        UUID sessionId = UUID.randomUUID();

        String token = jwtService.generateCustomerToken(customer, sessionId);
        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.get("sessionId", String.class)).isEqualTo(sessionId.toString());
    }

    @Test
    void generateToken_adminRole_isPreserved() {
        Customer admin = customer(1L, "admin@example.com", Role.ADMIN);
        String token = jwtService.generateCustomerToken(admin, UUID.randomUUID());

        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void extractClaims_invalidToken_throws() {
        assertThatThrownBy(() -> jwtService.extractClaims("not.a.valid.token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractClaims_expiredToken_throws() {
        AppProperties expiredProps = new AppProperties(
                new AppProperties.Jwt(TEST_SECRET, -1000L), // already expired
                new AppProperties.Activation("http://localhost/activate", "http://localhost/success", "http://localhost/fail", 24),
                new AppProperties.Security(5, 7, 24),
                new AppProperties.Agreements(List.of()),
                new AppProperties.Messages("", ""),
                new AppProperties.Cleanup(new AppProperties.Cleanup.RefreshToken(1000)),
                new AppProperties.Internal("key")
        );
        JwtService expiredJwtService = new JwtService(expiredProps.jwt());
        Customer customer = customer(1L, "user@example.com", Role.CUSTOMER);
        String token = expiredJwtService.generateCustomerToken(customer, UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.extractClaims(token))
                .isInstanceOf(JwtException.class);
    }

    private static Customer customer(Long id, String email, Role role) {
        Customer c = new Customer();
        c.setRole(role);
        c.setEmail(email);
        c.setPassword("hashed");
        c.setFirstName("Test");
        c.setLastName("User");
        c.setPhone("+1234567890");
        c.setEnabled(true);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
