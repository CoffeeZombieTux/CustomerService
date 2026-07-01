package io.customerservice.customerservice.repository;

import io.customerservice.customerservice.entity.RefreshToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    boolean existsBySessionId(UUID sessionId);

    int countByCustomerId(Long customerId);

    void deleteByCustomerId(Long customerId);

    Optional<RefreshToken> findByIdAndCustomerId(Long id, Long customerId);

    List<RefreshToken> findAllByCustomerIdAndExpiredAtAfter(Long customerId, OffsetDateTime threshold);

    @Query("SELECT r.id FROM RefreshToken r WHERE r.expiredAt < :threshold")
    List<Long> findExpiredIds(@Param("threshold") OffsetDateTime threshold, Pageable pageable);

    void deleteByIdIn(Collection<Long> ids);
}