package io.customerservice.customerservice.config;

import io.customerservice.customerservice.entity.AgreementType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Activation activation,
        Security security,
        Agreements agreements,
        Messages messages,
        Cleanup cleanup,
        Internal internal
) {
    public record Jwt(String secret, long expirationMs) {}

    public record Activation(String buttonUrl, String successUrl, String failUrl, int tokenLivenessHours) {}

    public record Security(int maxSessions, int refreshTokenLivenessDays, int idempotencyKeyHours) {}

    public record Agreements(List<AgreementType> mandatory) {}

    public record Messages(String registrationSuccess, String resendActivation) {}

    public record Cleanup(RefreshToken refreshToken) {
        public record RefreshToken(int batch) {}
    }

    public record Internal(String apiKey) {}
}
