package io.customerservice.customerservice.repository;

import io.customerservice.customerservice.entity.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByToken(UUID token);
    long deleteByExpiredAtBefore(OffsetDateTime threshold);
}
